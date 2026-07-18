"""
Lesson 7 — Evaluating retrieval: turn "seems good" into a number
(no API key required; optional embedding comparison needs a lesson-6 backend)

Big idea: so far we've judged retrieval by eyeballing results. That doesn't
scale and it doesn't let you compare two systems honestly. Real RAG work lives
and dies on measurement: you build a small labeled test set, run your retriever
over it, and compute metrics. Then every change — a new chunking strategy, a
different embedding model, a bigger top_k — becomes a number that went up or
down instead of a vibe.

We measure retrieval with two standard metrics:

  Hit@k  — of all test questions, what fraction have the correct document
           somewhere in the top k results? (Also called recall@k when there's
           one relevant doc per question.) Higher is better; 1.0 is perfect.

  MRR    — Mean Reciprocal Rank. For each question, score 1/rank of the first
           correct result (1.0 if it's #1, 0.5 if #2, 0.33 if #3, 0 if missing),
           then average. Rewards putting the right doc HIGH, not just present.

Run:  python lesson_07_evaluation.py
"""

from rag_lib import Retriever, CORPUS_DIR


# ---------------------------------------------------------------------------
# The labeled test set: questions paired with the document that answers them.
# This is the single most valuable artifact in a real RAG project. Build it by
# hand, keep it in version control, and grow it every time you find a failure.
# ---------------------------------------------------------------------------
EVAL_SET = [
    ("What are the three core objects in Nimbus?",            "01_overview.md"),
    ("How much does the Cumulus tier cost per hour?",         "02_pricing.md"),
    ("Is there a free tier?",                                 "02_pricing.md"),
    ("Do I get a discount for paying annually?",              "02_pricing.md"),
    ("How do I rotate an access key without downtime?",       "03_access_keys.md"),
    ("How fast do revoked keys stop working?",                "03_access_keys.md"),
    ("What permission levels can an access key have?",        "03_access_keys.md"),
    ("What is the default index type?",                       "04_indexing.md"),
    ("Which distance metrics are supported?",                 "04_indexing.md"),
    ("Why am I getting a 429 rate_limited error?",            "05_troubleshooting.md"),
    ("What batch size should I use for bulk inserts?",        "05_troubleshooting.md"),
    ("Which region is certified for GDPR?",                   "06_regions.md"),
    ("Can I move a collection to another region?",            "06_regions.md"),
    ("What tier do I need for cross-region replication?",     "06_regions.md"),

    # --- Reworded questions: same meaning, deliberately different words. -----
    # These use synonyms the docs don't contain, so they stress a lexical
    # (word-overlap) retriever like TF-IDF. This is where you expect misses —
    # and exactly where a real embedding model (lesson 6) should do better.
    ("How do I swap my credentials safely?",                  "03_access_keys.md"),
    ("What does it cost to run the biggest plan?",            "02_pricing.md"),
    ("Is my data copied to another location automatically?",  "06_regions.md"),
    ("Which datacenter serves European customers?",           "06_regions.md"),
    ("My uploads are painfully slow, how do I speed them up?", "05_troubleshooting.md"),
    ("How do I keep my login secret from being abused?",      "03_access_keys.md"),
    # The answer (data-transfer cost) lives in the pricing doc, but the word
    # "regions" pulls the regions doc to #1 — so the right doc lands at rank 2.
    # A textbook case for why Hit@3 can beat Hit@1.
    ("How is data transfer between regions billed?",          "02_pricing.md"),
]


def rank_of_correct(ranked_doc_ids, expected: str):
    """1-indexed position of the expected doc, or None if it's not in the list."""
    for i, doc_id in enumerate(ranked_doc_ids, start=1):
        if doc_id == expected:
            return i
    return None


def evaluate(ranked_fn, ks=(1, 3, 5)):
    """Run every eval question and compute Hit@k for each k, plus MRR.

    ranked_fn(query) must return a list of doc_ids in ranked order (best first).
    """
    max_k = max(ks)
    hits = {k: 0 for k in ks}
    reciprocal_ranks = []
    per_question = []   # (question, expected, rank_or_None)

    for question, expected in EVAL_SET:
        ranked = ranked_fn(question, max_k)
        rank = rank_of_correct(ranked, expected)
        per_question.append((question, expected, rank))

        # Reciprocal rank for MRR (0 if the correct doc never showed up).
        reciprocal_ranks.append(1.0 / rank if rank else 0.0)
        # Hit@k for each threshold.
        for k in ks:
            if rank is not None and rank <= k:
                hits[k] += 1

    n = len(EVAL_SET)
    hit_rates = {k: hits[k] / n for k in ks}
    mrr = sum(reciprocal_ranks) / n
    return hit_rates, mrr, per_question


def print_report(name, hit_rates, mrr, per_question):
    print(f"\n{name}")
    print("-" * len(name))
    for k, rate in sorted(hit_rates.items()):
        print(f"  Hit@{k}: {rate:.2f}  ({int(rate * len(EVAL_SET))}/"
              f"{len(EVAL_SET)} questions)")
    print(f"  MRR:   {mrr:.3f}")

    # Show any question whose correct doc wasn't the #1 result — these are the
    # ones to investigate. Failures are where you learn to improve the system.
    misses = [(q, exp, r) for q, exp, r in per_question if r != 1]
    if misses:
        print("  Not ranked #1 (investigate these):")
        for q, exp, rank in misses:
            where = f"rank {rank}" if rank else "NOT in top results"
            print(f"    - {where:<18} expected {exp}: {q}")
    else:
        print("  Every question's correct doc was ranked #1.")


def main() -> None:
    tfidf = Retriever(CORPUS_DIR)

    def tfidf_ranked(query, k):
        return [r.chunk.doc_id for r in tfidf.search(query, top_k=k)]

    print("=" * 70)
    print(f"Evaluating retrieval over {len(EVAL_SET)} labeled questions")
    print("=" * 70)

    hit_rates, mrr, per_q = evaluate(tfidf_ranked)
    print_report("TF-IDF (from-scratch, lesson 1-2)", hit_rates, mrr, per_q)

    # --- Optional: measure the lesson-6 upgrade on the SAME test set --------
    # This is the real reason you build an eval set: to decide, with evidence,
    # whether a change actually helps. If a lesson-6 backend is installed, we
    # compare embeddings head-to-head against TF-IDF.
    try:
        from lesson_06_real_embeddings import get_embedder, EmbeddingRetriever
        name, embed = get_embedder()
    except Exception:  # noqa: BLE001
        name, embed = None, None

    if embed is not None:
        emb = EmbeddingRetriever(CORPUS_DIR, embed)

        def emb_ranked(query, k):
            return [chunk.doc_id for _, chunk in emb.search(query, top_k=k)]

        hit_rates_e, mrr_e, per_q_e = evaluate(emb_ranked)
        print_report(f"Real embeddings ({name})", hit_rates_e, mrr_e, per_q_e)
        print("\nCompare the two reports above. If embeddings win on Hit@1 or")
        print("MRR here, that's the evidence that justifies the added cost and")
        print("dependency — not a hunch. THIS is how retrieval choices get made.")
    else:
        print("\n(Install a lesson-6 embedding backend to see a head-to-head")
        print(" comparison of TF-IDF vs. real embeddings on this same test set.)")

    print("\n" + "=" * 70)
    print("How to use this in practice")
    print("=" * 70)
    print("  1. Every time retrieval fails a real user, add that question +")
    print("     its correct doc to EVAL_SET. Your test set becomes a memory of")
    print("     past mistakes the system must never repeat.")
    print("  2. Change ONE thing (chunk size, top_k, embedding model), re-run,")
    print("     and keep the change only if the numbers improve.")
    print("  3. This measures RETRIEVAL. To also grade the final ANSWER (did")
    print("     Claude use the context correctly?), the standard next step is")
    print("     'LLM-as-judge': ask a model to score each answer against the")
    print("     source. That closes the loop from retrieval quality to answer")
    print("     quality.")


if __name__ == "__main__":
    main()
