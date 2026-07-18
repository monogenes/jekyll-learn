"""
Lesson 4 — Tool use: letting Claude take actions (needs ANTHROPIC_API_KEY)

Big idea: by itself, a model can only produce text. "Tools" let it ASK your
program to do something — run a calculation, call an API, query a database —
and then continue reasoning with the result. This is the single mechanism that
turns a chatbot into an agent.

The loop is always the same:
    1. You send the question plus a list of tools Claude may use.
    2. Claude replies. If it wants a tool, the response's stop_reason is
       "tool_use" and it includes the tool name + inputs.
    3. YOUR code runs the tool and sends the result back.
    4. Repeat until Claude stops asking for tools (stop_reason "end_turn").

We use a trivial calculator tool here so the mechanics are crystal clear.
Lesson 5 swaps in a real tool (document retrieval) to build an agent.

Run:  python lesson_04_tools.py
"""

import os
import sys

import anthropic

MODEL = "claude-opus-4-8"

# --- 1. Define the tool: a name, a description, and a JSON input schema. ------
# The description is how Claude decides WHEN to use the tool, so write it well.
TOOLS = [
    {
        "name": "calculator",
        "description": "Evaluate a basic arithmetic expression and return the "
                       "numeric result. Use this for any math instead of "
                       "computing it yourself.",
        "input_schema": {
            "type": "object",
            "properties": {
                "expression": {
                    "type": "string",
                    "description": "A Python arithmetic expression, "
                                   "e.g. '(1200 * 24) * 30'",
                }
            },
            "required": ["expression"],
        },
    }
]


def run_tool(name: str, tool_input: dict) -> str:
    """Execute a tool call. Returns the result as a string."""
    if name == "calculator":
        expr = tool_input["expression"]
        # NOTE: eval on model output is fine ONLY for this toy demo. In real
        # code, never eval untrusted input — parse it or use a safe evaluator.
        try:
            return str(eval(expr, {"__builtins__": {}}, {}))
        except Exception as exc:  # noqa: BLE001
            return f"error: {exc}"
    return f"error: unknown tool {name}"


def answer(client: anthropic.Anthropic, question: str) -> str:
    """Run the agentic tool loop until Claude produces a final answer."""
    messages = [{"role": "user", "content": question}]

    while True:
        response = client.messages.create(
            model=MODEL,
            max_tokens=600,
            tools=TOOLS,
            messages=messages,
        )

        # Claude is done — no more tools requested.
        if response.stop_reason != "tool_use":
            return "".join(b.text for b in response.content if b.type == "text")

        # Otherwise, record Claude's turn (which contains the tool_use blocks)
        # and then run every tool it asked for.
        messages.append({"role": "assistant", "content": response.content})

        tool_results = []
        for block in response.content:
            if block.type == "tool_use":
                print(f"    [Claude calls {block.name}({block.input})]")
                result = run_tool(block.name, block.input)
                print(f"    [tool returns {result}]")
                tool_results.append({
                    "type": "tool_result",
                    "tool_use_id": block.id,   # must match the tool_use block
                    "content": result,
                })

        # Send all tool results back in a single user message, then loop.
        messages.append({"role": "user", "content": tool_results})


def main() -> None:
    if not os.environ.get("ANTHROPIC_API_KEY"):
        sys.exit("Set ANTHROPIC_API_KEY first. See the lesson docstring.")

    client = anthropic.Anthropic()

    question = (
        "A Nimbus Stratus cluster costs $0.90/hour. If I run 3 clusters "
        "continuously for 30 days, what's the total cost? Show the number."
    )
    print(f"Q: {question}\n")
    print(answer(client, question))


if __name__ == "__main__":
    main()
