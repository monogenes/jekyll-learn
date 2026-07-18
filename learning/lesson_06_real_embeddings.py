"""
Lesson 6 — Real embeddings: upgrading the engine (needs ONE backend, below)

Big idea: back in lesson 1 our from-scratch TF-IDF vectorizer had a blind spot.
"rotate an access key" and "swap my login credential" mean the same thing but
share no words, so TF-IDF scored them 0.000 — it matches spelling, not meaning.

Real embedding models fix this. They're neural networks trained so that text
with similar MEANING gets similar vectors, even with zero shared words. In this
lesson we drop a real embedding model into the exact same Retriever shape from
rag_lib.py and watch the blind spot disappear. The rest of the course
(lessons 3-5) would work unchanged on top of this — that's the whole point:
same architecture, smarter vectors.

Pick ONE backend:

  A) Voyage AI  (cloud, Anthropic's recommended embedding partner)
       pip install voyageai
       export VOYAGE_API_KEY=pa-...          # from dash.voyageai.com
       Uses the network, costs a tiny amount per run.

  B) sentence-transformers  (local, free, no key — downloads a model once)
       pip install sentence-transformers
       First run downloads ~90 MB; after that it's offline and free.

This script auto-detects whichever you installed. If you installed neither, it
prints these instructions and exits.

Run:  python lesson_06_real_embeddings.py
"""

import math
import os
import sys

from rag_lib import Chunk, load_corpus, Retriever, CORPUS_DIR


# ---------------------------------------------------------------------------
# Backend detection: use Voyage if a key is present, else sentence-transformers.
# Each backend just needs to turn a list of strings into a list of vectors.
# ---------------------------------------------------------------------------

def get_embedder():
    """Return (name, embed_fn) where embed_fn(texts, is_query) -> list[list]."""
    # --- Backend A: Voyage AI ---------------------------------------------
    if os.environ.get("VOYAGE_API_KEY"):
        try:
            import voyageai
        except ImportError:
            pass
        else:
            client = voyageai.Client()   # reads VOYAGE_API_KEY
            model = "voyage-3-lite"       # small, fast, cheap; swap if you like

            def embed(texts, is_query=False):
                # Voyage lets you tag whether text is a search query or a stored
                # document; using the right tag improves retrieval quality.
                input_type = "query" if is_query else "document"
                result = client.embed(texts, model=model, input_type=input_type)
                return result.embeddings

            return f"Voyage AI ({model})", embed

    # --- Backend B: sentence-transformers ---------------------------------
    try:
        from sentence_transformers import SentenceTransformer
    except ImportError:
        return None, None

    model_name = "all-MiniLM-L6-v2"       # classic 384-dim model, reliable
    model = SentenceTransformer(model_name)

    def embed(texts, is_query=False):
        # .encode returns a numpy array; convert to plain Python lists so the
        # rest of this file has no numpy dependency.
        return [vec.tolist() for vec in model.encode(texts)]

    return f"sentence-transformers ({model_name})", embed


# ---------------------------------------------------------------------------
# Dense cosine similarity. Lesson 1's version worked on sparse dicts; real
# embeddings are dense lists of floats, so here's the list version.
# ---------------------------------------------------------------------------

def dense_cosine(a, b) -> float:
    dot = sum(x * y for x, y in zip(a, b))
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(y * y for y in b))
    if na == 0 or nb == 0:
        return 0.0
    return dot / (na * nb)


# ---------------------------------------------------------------------------
# The SAME retriever, powered by a real embedding model. Note the interface:
# .search(query, top_k) -> results, identical to rag_lib.Retriever. Lessons
# 3-5 could import this class instead and work with no other changes.
# ---------------------------------------------------------------------------

class EmbeddingRetriever:
    def __init__(self, corpus_dir: str, embed_fn) -> None:
        self.embed = embed_fn
        self.chunks = load_corpus(corpus_dir)
        # Embed every chunk once, up front (documents, not queries).
        texts = [c.text for c in self.chunks]
        self.vectors = self.embed(texts, is_query=False)

    def search(self, query: str, top_k: int = 3):
        q_vec = self.embed([query], is_query=True)[0]
        scored = [
            (dense_cosine(q_vec, vec), chunk)
            for vec, chunk in zip(self.vectors, self.chunks)
        ]
        scored.sort(key=lambda pair: pair[0], reverse=True)
        return scored[:top_k]


def main() -> None:
    name, embed = get_embedder()
    if embed is None:
        sys.exit(
            "No embedding backend found. Install ONE of these, then re-run:\n\n"
            "  A) Voyage (cloud):    pip install voyageai && "
            "export VOYAGE_API_KEY=pa-...\n"
            "  B) Local (free):      pip install sentence-transformers\n\n"
            "See this file's docstring for details."
        )
    print(f"Using backend: {name}\n")

    # --- Part 1: the lesson-1 blind spot, revisited -----------------------
    print("=" * 70)
    print("PART 1 — the same-meaning-different-words pair that TF-IDF scored 0")
    print("=" * 70)
    sentences = [
        "How do I rotate a Nimbus access key?",              # 0
        "Swapping login credentials for the service safely.",  # 2 from lesson 1
    ]
    vecs = embed(sentences, is_query=False)
    score = dense_cosine(vecs[0], vecs[1])
    print(f"\n  '{sentences[0]}'")
    print(f"  '{sentences[1]}'")
    print(f"\n  TF-IDF similarity (lesson 1):      0.000")
    print(f"  Real-embedding similarity:         {score:.3f}")
    print("\n  The model recognizes these mean the same thing despite sharing")
    print("  no words. That is 'semantic' matching — the upgrade over lexical.")

    # --- Part 2: retrieval on a deliberately reworded query ---------------
    print("\n" + "=" * 70)
    print("PART 2 — retrieval when the question doesn't reuse the doc's words")
    print("=" * 70)
    # The access-key doc says "rotate an access key". This query says none of
    # those words. Watch how the two retrievers handle it.
    tricky_query = "How do I change my login credentials without an outage?"
    print(f"\nQuery: {tricky_query}\n")

    tfidf = Retriever(CORPUS_DIR)
    emb = EmbeddingRetriever(CORPUS_DIR, embed)

    print("TF-IDF (lexical) top 3:")
    for score, chunk in [(r.score, r.chunk) for r in tfidf.search(tricky_query)]:
        print(f"  {score:.3f}  [{chunk.doc_id}]  {chunk.text.split(chr(10))[0][:60]}")

    print("\nReal embeddings (semantic) top 3:")
    for score, chunk in emb.search(tricky_query):
        print(f"  {score:.3f}  [{chunk.doc_id}]  {chunk.text.split(chr(10))[0][:60]}")

    print("\nTakeaway: on reworded questions, semantic retrieval tends to surface")
    print("the right document even with little word overlap. This is why real")
    print("systems use embedding models — and why the Retriever INTERFACE, not")
    print("the vectorizer, is the part of RAG worth learning deeply.")


if __name__ == "__main__":
    main()
