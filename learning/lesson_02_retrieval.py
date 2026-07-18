"""
Lesson 2 — Retrieval: finding the right documents (no API key required)

Big idea: RAG = Retrieval + Augmented + Generation. This lesson is the
"Retrieval" part. Given a question, we search a corpus and return the few
chunks most likely to contain the answer. That's it — no language model yet.

Retrieval is the unglamorous half of RAG, but it's where most real systems
succeed or fail. If retrieval hands the model the wrong chunks, no amount of
clever prompting will save the answer.

Run me:  python lesson_02_retrieval.py
"""

from rag_lib import Retriever, CORPUS_DIR


def main() -> None:
    # Building the retriever loads every .md file in sample_corpus/, splits
    # each into chunks, and vectorizes them. Done once, reused for every query.
    retriever = Retriever(CORPUS_DIR)
    print(f"Indexed {len(retriever.chunks)} chunks from the sample corpus.\n")

    questions = [
        "How do I rotate an access key safely?",
        "How much does the Cumulus tier cost per hour?",
        "Why am I getting a 400 dimension_mismatch error?",
        "Can I move a collection from us-east to eu-west?",
    ]

    for question in questions:
        print("=" * 70)
        print(f"QUERY: {question}")
        print("=" * 70)
        results = retriever.search(question, top_k=3)
        for rank, result in enumerate(results, start=1):
            snippet = " ".join(result.chunk.text.split())[:100]
            print(f"  #{rank}  score={result.score:.3f}  "
                  f"[{result.chunk.doc_id}]")
            print(f"      {snippet}...")
        print()

    print("Notice: the top result for each question comes from the RIGHT")
    print("document, even though we never told the system which doc was which.")
    print("Word overlap alone gets us surprisingly far on a small corpus.")
    print()
    print("Experiment: change top_k, add your own question, or edit a doc in")
    print("sample_corpus/ and watch the rankings shift.")


if __name__ == "__main__":
    main()
