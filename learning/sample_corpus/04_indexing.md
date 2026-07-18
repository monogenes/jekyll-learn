# Nimbus DB — Indexing and Similarity Search

Nimbus builds an approximate-nearest-neighbor index over the embeddings you
store. You choose the index type when you create a collection; it cannot be
changed later.

- **flat** — exact search, no approximation. Best for collections under
  100,000 documents. Slowest at large scale but always returns the true
  top-k results.
- **hnsw** — graph-based approximate search. The default. Fast at millions of
  documents; may miss a small fraction of true neighbors.

## Distance metrics

Nimbus supports three distance metrics, set per collection:

- **cosine** — the default. Measures the angle between vectors; ignores their
  length. Best for text embeddings.
- **dot** — dot product. Sensitive to vector magnitude.
- **euclidean** — straight-line distance.

## Query parameters

A similarity query takes a `top_k` (how many results to return, max 100) and an
optional `min_score` filter. Results come back sorted by score, highest first.
Embeddings must have the same number of dimensions as the collection was
created with; a mismatch returns a `400 dimension_mismatch` error.
