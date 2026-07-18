"""
Lesson 1 — What is an embedding? (no API key required)

Big idea: computers can't compare *text* directly, but they can compare
*numbers*. An "embedding" is just a list of numbers (a vector) that represents
a piece of text. If two texts mean similar things, their vectors point in
similar directions — and we measure "similar direction" with cosine similarity.

Run me:  python lesson_01_embeddings.py

You will NOT call any API in this lesson. We use the tiny TF-IDF vectorizer
from rag_lib.py so you can watch text become vectors with nothing hidden.
"""

from rag_lib import TfidfVectorizer, Chunk, cosine_similarity


def main() -> None:
    # Four sentences, chosen to reveal both what TF-IDF does well and where it
    # breaks down:
    #   0 and 1 mean the same thing AND share words  -> should score high
    #   0 and 2 mean the same thing but share NO words -> TF-IDF misses it
    #   0 and 3 are unrelated                          -> should score low
    sentences = [
        "How do I rotate a Nimbus access key?",                  # 0
        "What are the steps to rotate a Nimbus access key?",     # 1  (same words)
        "Swapping login credentials for the service safely.",    # 2  (same idea, new words)
        "Nimbus pricing is billed per cluster per hour.",        # 3  (unrelated)
    ]

    # The vectorizer needs to "see" a corpus to learn which words are common.
    # We fit it on these sentences themselves for this small demo.
    corpus = [Chunk(doc_id="demo", title="demo", text=s) for s in sentences]
    vectorizer = TfidfVectorizer().fit(corpus)

    vectors = [vectorizer.vectorize(s) for s in sentences]

    print("=" * 70)
    print("STEP 1 — text becomes a vector (a dict of word -> weight)")
    print("=" * 70)
    for i, (sentence, vec) in enumerate(zip(sentences, vectors)):
        print(f"\n[{i}] {sentence!r}")
        # Show the 5 heaviest words so the vector is readable.
        top = sorted(vec.items(), key=lambda kv: kv[1], reverse=True)[:5]
        for term, weight in top:
            print(f"      {term:<12} {weight:.3f}")

    print("\n" + "=" * 70)
    print("STEP 2 — compare vectors with cosine similarity (0 = unrelated,")
    print("         1 = identical direction)")
    print("=" * 70)
    labels = {
        (0, 1): "same meaning, shared words",
        (0, 2): "same meaning, DIFFERENT words",
        (0, 3): "unrelated",
    }
    for (i, j), label in labels.items():
        score = cosine_similarity(vectors[i], vectors[j])
        print(f"  sim({i}, {j}) = {score:.3f}   ({label})")

    print("\nTakeaways:")
    print("  - 0 vs 1 scores HIGH: they share the words 'rotate/nimbus/access")
    print("    /key', and TF-IDF is built on shared words. Good.")
    print("  - 0 vs 3 scores LOW: barely any shared words. Correctly unrelated.")
    print("  - 0 vs 2 ALSO scores low -- even though they mean the same thing!")
    print("    'swap...login credential' and 'rotate...access key' overlap by")
    print("    almost no words, so TF-IDF can't see they're synonyms.")
    print("    THIS BLIND SPOT is exactly why real embedding models exist: they")
    print("    map meaning, not spelling, so 0 and 2 would land close together.")
    print("    Same code shape, smarter vectors -- see README 'Where to go next'.")


if __name__ == "__main__":
    main()
