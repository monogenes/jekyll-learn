"""
Lesson 3 — RAG: grounding Claude in your documents (needs ANTHROPIC_API_KEY)

Big idea: a language model only knows what was in its training data. It has
never heard of our fictional "Nimbus DB", so on its own it will either refuse
or hallucinate. RAG fixes this: we RETRIEVE the relevant chunks (lesson 2) and
paste them into the prompt as CONTEXT, then ask Claude to answer using only
that context. Retrieval + Augmented prompt + Generation = RAG.

This lesson shows the same question answered two ways so the difference is
obvious: once WITHOUT context, once WITH retrieved context.

Setup:
    pip install -r requirements.txt
    export ANTHROPIC_API_KEY=sk-ant-...      # from console.anthropic.com
Run:
    python lesson_03_rag.py
"""

import os
import sys

import anthropic

from rag_lib import Retriever, CORPUS_DIR

# Default to the most capable model. For cheap, high-volume experimentation
# while learning, you can switch this to "claude-haiku-4-5".
MODEL = "claude-opus-4-8"


def ask_without_context(client: anthropic.Anthropic, question: str) -> str:
    """Baseline: ask Claude with no documents. Watch it struggle."""
    response = client.messages.create(
        model=MODEL,
        max_tokens=400,
        messages=[{"role": "user", "content": question}],
    )
    return "".join(b.text for b in response.content if b.type == "text")


def ask_with_rag(client: anthropic.Anthropic, retriever: Retriever,
                 question: str) -> str:
    """The RAG pattern: retrieve, stuff into the prompt, then generate."""
    # 1. RETRIEVE the most relevant chunks for this question.
    results = retriever.search(question, top_k=3)

    # 2. AUGMENT: build a context block out of those chunks. Labeling each
    #    source lets the model cite where an answer came from.
    context = "\n\n".join(
        f"[Source: {r.chunk.doc_id}]\n{r.chunk.text}" for r in results
    )

    # A system prompt sets the rules: answer ONLY from context, and admit when
    # the answer isn't there. This is what prevents confident hallucination.
    system = (
        "You are a support assistant for Nimbus DB. Answer the user's question "
        "using ONLY the context provided. If the answer is not in the context, "
        "say you don't know. Cite the source filename(s) you used."
    )

    # 3. GENERATE. The retrieved context goes in the user message alongside
    #    the question.
    user_message = f"Context:\n{context}\n\nQuestion: {question}"
    response = client.messages.create(
        model=MODEL,
        max_tokens=400,
        system=system,
        messages=[{"role": "user", "content": user_message}],
    )
    answer = "".join(b.text for b in response.content if b.type == "text")

    retrieved = ", ".join(r.chunk.doc_id for r in results)
    return f"{answer}\n\n(retrieved chunks from: {retrieved})"


def main() -> None:
    if not os.environ.get("ANTHROPIC_API_KEY"):
        sys.exit("Set ANTHROPIC_API_KEY first. See the lesson docstring.")

    client = anthropic.Anthropic()
    retriever = Retriever(CORPUS_DIR)

    question = "How do I rotate a Nimbus access key without downtime?"

    print("=" * 70)
    print("WITHOUT RAG (no documents given to the model)")
    print("=" * 70)
    print(ask_without_context(client, question))

    print("\n" + "=" * 70)
    print("WITH RAG (relevant Nimbus docs retrieved and added to the prompt)")
    print("=" * 70)
    print(ask_with_rag(client, retriever, question))

    print("\n" + "-" * 70)
    print("Try more questions:")
    for q in [
        "What does the Cumulus tier cost and what are its limits?",
        "I got a 401 invalid_key error. What could be wrong?",
        "Does Nimbus have a free tier?",              # tests 'I don't know'
        "What is the capital of France?",             # off-topic on purpose
    ]:
        print(f"\nQ: {q}")
        print(ask_with_rag(client, retriever, q))


if __name__ == "__main__":
    main()
