"""
rag_lib.py — shared building blocks for the RAG + agents course.

This module deliberately has ZERO external dependencies. It implements a tiny
TF-IDF vectorizer and a cosine-similarity retriever in plain Python so you can
see exactly how "vector search" works under the hood before you ever call an
API or install a library.

In the real world you would replace the toy vectorizer here with a proper
embedding model (see the README's "Where to go next" section). But the *shape*
of the code — turn text into vectors, compare vectors, return the closest
documents — is exactly the same. That is the whole point.
"""

from __future__ import annotations

import math
import os
import re
from dataclasses import dataclass, field


# ---------------------------------------------------------------------------
# 1. Loading and chunking the corpus
# ---------------------------------------------------------------------------

@dataclass
class Chunk:
    """One retrievable piece of text, with a pointer back to its source doc."""
    doc_id: str          # e.g. "03_access_keys.md"
    title: str           # first heading in the doc
    text: str            # the chunk's text
    vector: dict = field(default_factory=dict)  # filled in by the vectorizer


def _tokenize(text: str) -> list[str]:
    """Lowercase and split into word tokens. The simplest thing that works."""
    return re.findall(r"[a-z0-9]+", text.lower())


def load_corpus(corpus_dir: str) -> list[Chunk]:
    """Read every .md file in a directory and split it into chunks.

    We chunk by paragraph (blank-line separated). Chunking matters a lot in
    real RAG systems: too big and you stuff irrelevant text into the prompt,
    too small and you lose context. Paragraph-level is a reasonable default.
    """
    chunks: list[Chunk] = []
    for name in sorted(os.listdir(corpus_dir)):
        if not name.endswith(".md"):
            continue
        path = os.path.join(corpus_dir, name)
        with open(path, "r", encoding="utf-8") as f:
            raw = f.read()

        # The title is the first "# heading" line, if any.
        title_match = re.search(r"^#\s+(.+)$", raw, re.MULTILINE)
        title = title_match.group(1).strip() if title_match else name

        # Split on blank lines into paragraphs, drop pure-heading fragments.
        for para in re.split(r"\n\s*\n", raw):
            para = para.strip()
            if len(para) < 25:      # skip tiny fragments / lone headings
                continue
            chunks.append(Chunk(doc_id=name, title=title, text=para))
    return chunks


# ---------------------------------------------------------------------------
# 2. A from-scratch TF-IDF vectorizer
# ---------------------------------------------------------------------------
# TF-IDF ("term frequency times inverse document frequency") turns a piece of
# text into a vector of numbers. Each dimension is a word; the value is how
# important that word is to this chunk relative to the whole corpus.
#   - term frequency: words that appear often in THIS chunk score higher.
#   - inverse document frequency: words that appear in MANY chunks (like "the")
#     score lower, because they don't help tell chunks apart.
#
# This is a *lexical* representation: it only knows about literal word overlap.
# Real embedding models capture *meaning* (so "car" and "automobile" would be
# close even with no shared letters). But TF-IDF is enough to make retrieval
# work and to teach the mechanics of vectors and similarity.

class TfidfVectorizer:
    def __init__(self) -> None:
        self.idf: dict[str, float] = {}
        self._fitted = False

    def fit(self, chunks: list[Chunk]) -> "TfidfVectorizer":
        """Learn the inverse-document-frequency weight of every word."""
        num_docs = len(chunks)
        doc_freq: dict[str, int] = {}
        for chunk in chunks:
            for term in set(_tokenize(chunk.text)):
                doc_freq[term] = doc_freq.get(term, 0) + 1
        # Smoothed idf so nothing is ever exactly zero.
        self.idf = {
            term: math.log((1 + num_docs) / (1 + df)) + 1.0
            for term, df in doc_freq.items()
        }
        self._fitted = True
        return self

    def vectorize(self, text: str) -> dict[str, float]:
        """Turn a string into a sparse, L2-normalized TF-IDF vector (a dict)."""
        if not self._fitted:
            raise RuntimeError("Call fit() before vectorize().")
        tokens = _tokenize(text)
        if not tokens:
            return {}
        # term frequency in this text
        tf: dict[str, int] = {}
        for tok in tokens:
            tf[tok] = tf.get(tok, 0) + 1
        # tf * idf (unknown words — not seen during fit — are ignored)
        vec = {
            term: count * self.idf.get(term, 0.0)
            for term, count in tf.items()
            if term in self.idf
        }
        # Normalize to unit length so cosine similarity is just a dot product.
        norm = math.sqrt(sum(w * w for w in vec.values()))
        if norm == 0:
            return {}
        return {term: w / norm for term, w in vec.items()}


def cosine_similarity(a: dict[str, float], b: dict[str, float]) -> float:
    """Cosine similarity of two sparse vectors = dot product of unit vectors.

    Ranges from 0 (no shared terms) to 1 (identical direction). Because both
    inputs are already normalized, we just sum the products of shared terms.
    """
    # Iterate the smaller dict for speed.
    if len(a) > len(b):
        a, b = b, a
    return sum(weight * b.get(term, 0.0) for term, weight in a.items())


# ---------------------------------------------------------------------------
# 3. The retriever — the "R" in RAG
# ---------------------------------------------------------------------------

@dataclass
class Result:
    chunk: Chunk
    score: float


class Retriever:
    """Indexes a corpus and returns the chunks most similar to a query."""

    def __init__(self, corpus_dir: str) -> None:
        self.chunks = load_corpus(corpus_dir)
        if not self.chunks:
            raise RuntimeError(f"No .md documents found in {corpus_dir!r}")
        self.vectorizer = TfidfVectorizer().fit(self.chunks)
        # Pre-compute a vector for every chunk once, up front.
        for chunk in self.chunks:
            chunk.vector = self.vectorizer.vectorize(chunk.text)

    def search(self, query: str, top_k: int = 3) -> list[Result]:
        """Return the top_k chunks most similar to the query, best first."""
        q_vec = self.vectorizer.vectorize(query)
        scored = [
            Result(chunk=chunk, score=cosine_similarity(q_vec, chunk.vector))
            for chunk in self.chunks
        ]
        scored.sort(key=lambda r: r.score, reverse=True)
        return scored[:top_k]


# Convenience: the path to the bundled sample corpus, relative to this file.
CORPUS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sample_corpus")
