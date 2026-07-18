# Exercises

Do these in order. Each builds the intuition you'll need for the next lesson.
Don't just read — change the code and re-run it. Breaking things and fixing
them is where the learning happens.

## After Lesson 1 (embeddings)

1. Add a fourth sentence that means the same as sentence 0 but shares *no*
   words with it (e.g. "steps to swap my login secret with no outage"). Run
   the script. Why does TF-IDF score it low even though it means the same
   thing? This limitation is exactly what real embedding models solve.
2. Add a sentence that shares many words with sentence 0 but means something
   different. Does cosine similarity get fooled? What does that tell you about
   lexical vs. semantic matching?

## After Lesson 2 (retrieval)

3. Add your own question to the `questions` list. Does the right document come
   back first? If not, why — is it a wording problem or a chunking problem?
4. Change `top_k` from 3 to 1. Then to 6. How does that trade off precision
   (only the best) vs. recall (don't miss anything)? In RAG, this trade-off
   directly controls what the model gets to see.
5. Edit `load_corpus` in `rag_lib.py` to chunk by sentence instead of by
   paragraph. Re-run. Do results get better or worse? Chunking strategy is one
   of the highest-leverage knobs in real RAG systems.

## After Lesson 3 (RAG)

6. Delete the `system` prompt's "say you don't know" instruction and ask a
   question the corpus can't answer. Does the model hallucinate now? Put it
   back. You've just seen why grounding instructions matter.
7. Print the retrieved `context` before the API call. When an answer is wrong,
   is it because retrieval fetched the wrong chunks, or because the model
   misread good chunks? Learning to tell these apart is the core RAG debugging
   skill.
8. Lower `top_k` to 1 and ask a question whose answer spans two documents
   (e.g. "which regions support GDPR *and* what does cross-region replication
   cost?"). Watch retrieval starve the model of half the answer.

## After Lesson 4 (tools)

9. Add a second tool, `get_current_time`, that returns the current time. Ask a
   question that needs it. Notice you only had to add a schema and a branch in
   `run_tool` — the loop didn't change.
10. Make the calculator raise on `1/0` and return the error string. Ask Claude
    to divide by zero. How does it recover? Good tools return errors as data,
    not exceptions.

## After Lesson 5 (agentic RAG)

11. Add a `print(messages)` at the end of `run_agent` for one question. Read
    the full transcript: user question, assistant tool calls, tool results,
    final answer. This message list IS the agent's memory.
12. Ask a question that needs two *different* searches (e.g. "compare the
    Spark and Cumulus query rate limits"). Does the agent search twice? Watch
    the `[step: ...]` lines.
13. Remove the `search_docs` tool and keep only the calculator. Ask a Nimbus
    question. What does the agent do when it lacks the tool it needs?
14. Swap the manual loop for the SDK's tool runner
    (`client.beta.messages.tool_runner` with `@beta_tool` functions). Confirm
    you get the same behavior with less code — now that you know what it's
    doing under the hood.

## Stretch project

15. Replace the toy TF-IDF vectorizer in `rag_lib.py` with real embeddings
    (see the README's "Where to go next"). Keep the `Retriever` interface
    identical. Every lesson should still run unchanged — proof that you
    understood the *shape* of RAG, not just one implementation.
