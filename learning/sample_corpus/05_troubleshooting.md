# Nimbus DB — Troubleshooting

## Error: 400 dimension_mismatch

The embedding you sent has a different number of dimensions than the
collection. Check that you are using the same embedding model for both writing
and querying. A collection's dimension is fixed at creation and cannot change.

## Error: 401 invalid_key

The `X-Nimbus-Key` header is missing, malformed, or points to a revoked key.
Confirm the key has not been rotated out, and that you are hitting the region
the collection lives in — a `us-east` key will not work against `eu-west`.

## Error: 429 rate_limited

You have exceeded your tier's request rate. Spark allows 10 queries/second,
Stratus 200/second, and Cumulus 2,000/second. Back off and retry; the
`Retry-After` header tells you how many seconds to wait.

## Queries return too few results

If `top_k` results come back empty or short, check your `min_score` filter —
setting it too high filters everything out. Also confirm the collection
actually contains documents; a newly created collection is empty until you
insert.

## Slow writes

Bulk inserts are fastest in batches of 100 to 500 documents. Inserting one
document per request is rate-limited and slow; use the batch endpoint instead.
