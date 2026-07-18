# Learn RAG & Agentic AI — a hands-on curriculum

You've been learning by building (that's what the Jekyll blog in this repo is).
This takes the same approach to two of the most useful ideas in applied AI
today: **RAG** (retrieval-augmented generation) and **agents**. You'll write
and run small, heavily-commented programs that build on each other, starting
from "what even is a vector" and ending with an agent that decides for itself
when to look things up.

Everything here is self-contained. Lessons 1–2 need **nothing but Python**.
Lessons 3–5 call **Claude** via the official Anthropic SDK.

---

## The mental model (read this first)

**RAG** answers the question: *how do I get a language model to answer using
knowledge it was never trained on* — your docs, your product, today's data?
You can't retrain the model for every question. Instead you:

1. **Retrieve** the few documents relevant to the question (search).
2. **Augment** the prompt by pasting those documents in as context.
3. **Generate** an answer, instructing the model to use only that context.

That's it. RAG is "open-book exam for a language model." The hard part isn't
the model — it's the *retrieval*: finding the right pages to put in front of it.

**Agents** answer a different question: *how do I let a model take actions and
make decisions* instead of just emitting text? You give it **tools** (functions
it can call), and it loops — think, call a tool, read the result, think again —
until it's done. **Agentic RAG** is the natural marriage: retrieval becomes one
of the tools the agent can choose to use.

```
  Lesson 1        Lesson 2         Lesson 3          Lesson 4        Lesson 5
  vectors    →    retrieval   →    basic RAG    →    tool use   →   agentic RAG
 (what is a      (find the        (retrieve +       (models that   (agent decides
  vector?)        right docs)      generate)         take actions)  when to retrieve)

  Lesson 6: real embeddings — swap the toy vectorizer for a trained model and
  watch lesson 1's blind spot disappear, with every other lesson unchanged.
  Lesson 7: evaluation — score retrieval on a labeled test set so every change
  (chunking, top_k, embedding model) becomes a measured win or loss.
```

---

## The lessons

| # | File | What you'll learn | Needs API key? |
|---|------|-------------------|:--------------:|
| 1 | `lesson_01_embeddings.py` | Text → vectors, and comparing them with cosine similarity | No |
| 2 | `lesson_02_retrieval.py` | Search a corpus for the chunks that answer a question | No |
| 3 | `lesson_03_rag.py` | Feed retrieved chunks to Claude to get grounded answers | Yes |
| 4 | `lesson_04_tools.py` | Let Claude call tools — the tool-use loop | Yes |
| 5 | `lesson_05_agent.py` | An agent that retrieves + calculates on its own | Yes |
| 6 | `lesson_06_real_embeddings.py` | Swap the toy vectorizer for a real embedding model — same interface, smarter vectors | One backend* |
| 7 | `lesson_07_evaluation.py` | Measure retrieval with a labeled test set (Hit@k, MRR) so improvements become numbers, not hunches | No** |

\* Lesson 6 needs either Voyage AI (`pip install voyageai` + a `VOYAGE_API_KEY`) **or** a free local model (`pip install sentence-transformers`). It auto-detects whichever you install. No Claude key required.

\*\* Lesson 7 runs fully offline on the TF-IDF retriever. If a lesson-6 backend is installed, it *also* prints a head-to-head TF-IDF-vs-embeddings comparison on the same test set — which is how you'd actually decide whether the upgrade is worth it.

Supporting files:

- `rag_lib.py` — the shared engine: corpus loading, a from-scratch TF-IDF
  vectorizer, and the retriever. Read it; it's short and has no magic.
- `sample_corpus/` — docs for a **fictional** product ("Nimbus DB"). It's made
  up on purpose: since no model was trained on it, RAG is the *only* way to
  answer questions about it. That makes the value of retrieval unmistakable.
- `exercises.md` — do these. Reading isn't learning; changing the code is.

---

## Setup

You already have a Python virtualenv in this repo (`../env`). From this
`learning/` directory:

```bash
# Lessons 1 and 2 — no setup needed, just run:
python lesson_01_embeddings.py
python lesson_02_retrieval.py

# Lessons 3–5 — install the SDK and set your key:
pip install -r requirements.txt
export ANTHROPIC_API_KEY=sk-ant-...        # get one at console.anthropic.com
python lesson_03_rag.py
```

Each run of lessons 3–5 costs a few cents. The lessons default to the
`claude-opus-4-8` model; while you're iterating, you can change the `MODEL`
constant at the top of each file to `claude-haiku-4-5` to run much cheaper.

> No key yet? You can still complete lessons 1 and 2 and read the code for 3–5
> — the retrieval half of RAG is fully runnable offline.

---

## Where to go next (upgrading the toy pieces to production)

This course keeps two things deliberately simple so you can see through them.
Here's how each maps to the real world:

- **Real embeddings instead of TF-IDF.** Our vectorizer only matches literal
  words. Production systems use a trained embedding model that captures
  *meaning* (so "access key" and "credential" are close). **Lesson 6 does
  exactly this** — it drops a real model (Voyage AI or a free local
  `sentence-transformers` model) into the same `Retriever` interface and shows
  the improvement directly. Anthropic recommends
  [Voyage AI](https://www.voyageai.com/) as its embedding partner.

- **A real vector database instead of a Python list.** We loop over every chunk
  in memory. That's fine for hundreds of docs; at millions you'd use a vector
  store (pgvector, Pinecone, Weaviate, Qdrant, Chroma, or Nimbus DB if it were
  real). They do the same cosine search, just indexed and scaled.

- **The SDK tool runner instead of a hand-written loop.** Lessons 4–5 write the
  agent loop by hand so you understand it. In real code, use
  `client.beta.messages.tool_runner` with `@beta_tool`-decorated functions and
  the SDK runs the loop for you.

- **Better chunking and re-ranking.** Two of the biggest levers in real RAG
  quality are how you split documents and whether you re-rank retrieved results
  before sending them. Lesson 7 gives you the scoreboard (Hit@k, MRR) to test
  both: change one thing, re-run, keep it only if the numbers go up.

- **Answer evaluation (LLM-as-judge).** Lesson 7 measures *retrieval*. The
  remaining frontier is grading the final *answer* — did Claude use the context
  faithfully? The standard technique is to have a model score each answer
  against its source. That's the natural lesson 8 once these seven click.

For deeper, always-current references, see Anthropic's docs on
[tool use](https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview)
and [building agents](https://platform.claude.com/docs/en/agents-and-tools).

---

## The one-sentence summary

RAG is giving a model the right documents at question time; agents are giving a
model tools and letting it decide what to do — and everything in this folder is
those two ideas, built up one runnable step at a time.
