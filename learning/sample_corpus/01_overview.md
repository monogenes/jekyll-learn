# Nimbus DB — Overview

Nimbus DB is a fictional managed vector database used in this course. It stores
"collections" of documents, each with an embedding, and supports similarity
search. Nimbus is region-based: every collection lives in exactly one region
(`us-east`, `eu-west`, or `ap-south`) and cannot be moved after creation.

The three core objects in Nimbus are:

- **Cluster** — the compute + storage unit you provision. Billed hourly.
- **Collection** — a named group of documents inside a cluster.
- **Access key** — the credential used by clients to read or write a collection.

Nimbus is intentionally made up. None of this is real, which is exactly why it
is useful for learning retrieval-augmented generation: a language model has
never seen it, so it can only answer questions about Nimbus if you *give it*
the relevant documents at query time.
