"""
Lesson 5 — Agentic RAG: an agent that decides when to look things up
(needs ANTHROPIC_API_KEY)

Big idea: in lesson 3 (basic RAG) *we* always retrieved before asking. But what
if a question needs two lookups, or a lookup plus a calculation, or no lookup
at all? An AGENT figures that out itself. We give Claude tools — here, a
document search tool and a calculator — and let it plan: it can search, read
the results, search again, do math, and only then answer.

This is "agentic RAG": retrieval becomes a tool the model calls on demand,
rather than a fixed step we hard-code. It's the same tool loop as lesson 4,
now with a tool that reaches into the corpus from lesson 2.

Run:  python lesson_05_agent.py
"""

import os
import sys

import anthropic

from rag_lib import Retriever, CORPUS_DIR

MODEL = "claude-opus-4-8"

# The retriever is created once and closed over by the tool function below.
retriever = Retriever(CORPUS_DIR)

TOOLS = [
    {
        "name": "search_docs",
        "description": "Search the Nimbus DB documentation for passages "
                       "relevant to a query. Returns the top matching chunks. "
                       "Call this whenever you need facts about Nimbus DB — "
                       "pricing, keys, regions, errors, indexing, etc.",
        "input_schema": {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "What to look up, in natural language.",
                }
            },
            "required": ["query"],
        },
    },
    {
        "name": "calculator",
        "description": "Evaluate a basic arithmetic expression. Use this for "
                       "any math rather than computing it yourself.",
        "input_schema": {
            "type": "object",
            "properties": {
                "expression": {"type": "string"},
            },
            "required": ["expression"],
        },
    },
]


def run_tool(name: str, tool_input: dict) -> str:
    if name == "search_docs":
        results = retriever.search(tool_input["query"], top_k=3)
        # Return the retrieved passages as text for the model to read.
        return "\n\n".join(
            f"[{r.chunk.doc_id} | score {r.score:.2f}]\n{r.chunk.text}"
            for r in results
        )
    if name == "calculator":
        try:
            return str(eval(tool_input["expression"], {"__builtins__": {}}, {}))
        except Exception as exc:  # noqa: BLE001
            return f"error: {exc}"
    return f"error: unknown tool {name}"


SYSTEM = (
    "You are a Nimbus DB support agent. Use the search_docs tool to ground "
    "every factual claim about Nimbus in the documentation; do not rely on "
    "prior knowledge, since Nimbus is proprietary. Use the calculator for "
    "arithmetic. If the docs don't contain the answer, say so. Cite the "
    "source filenames you used."
)


def run_agent(client: anthropic.Anthropic, question: str,
              max_steps: int = 8) -> str:
    """The agent loop: think -> maybe call tools -> repeat -> answer."""
    messages = [{"role": "user", "content": question}]

    for _ in range(max_steps):   # a step cap prevents runaway loops
        response = client.messages.create(
            model=MODEL,
            max_tokens=800,
            system=SYSTEM,
            tools=TOOLS,
            messages=messages,
        )

        if response.stop_reason != "tool_use":
            return "".join(b.text for b in response.content if b.type == "text")

        messages.append({"role": "assistant", "content": response.content})

        tool_results = []
        for block in response.content:
            if block.type == "tool_use":
                preview = str(block.input)[:80]
                print(f"    [step: {block.name}({preview})]")
                tool_results.append({
                    "type": "tool_result",
                    "tool_use_id": block.id,
                    "content": run_tool(block.name, block.input),
                })
        messages.append({"role": "user", "content": tool_results})

    return "(stopped: hit the step limit without finishing)"


def main() -> None:
    if not os.environ.get("ANTHROPIC_API_KEY"):
        sys.exit("Set ANTHROPIC_API_KEY first. See the lesson docstring.")

    client = anthropic.Anthropic()

    # These questions need different amounts of work — watch the [step: ...]
    # lines to see the agent plan. The last one needs a lookup AND math.
    questions = [
        "How do I rotate an access key, and how quickly do revoked keys stop working?",
        "I'm on Spark and getting 429 errors. Why, and what's my query limit?",
        "If I run 2 Cumulus clusters for a full 7-day week, how much will it cost?",
    ]
    for question in questions:
        print("=" * 70)
        print(f"USER: {question}")
        print("=" * 70)
        print(run_agent(client, question))
        print()

    print("-" * 70)
    print("What made this 'agentic': the model chose whether to search, what")
    print("to search for, whether to also do math, and when it had enough to")
    print("answer — all on its own. That decision-making is the leap from a")
    print("fixed RAG pipeline (lesson 3) to an agent.")
    print()
    print("Production note: the anthropic SDK ships a 'tool runner' that writes")
    print("this loop for you (client.beta.messages.tool_runner + @beta_tool).")
    print("We wrote the loop by hand so you can see exactly what it does.")


if __name__ == "__main__":
    main()
