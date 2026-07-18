# Nimbus DB — Access Keys and Security

Every request to Nimbus DB must include an access key in the `X-Nimbus-Key`
header. Access keys are scoped to a single collection and carry one of two
permission levels:

- **reader** — may run similarity queries and fetch documents.
- **writer** — may do everything a reader can, plus insert, update, and delete
  documents.

## Rotating an access key

Keys should be rotated every 90 days. To rotate a key without downtime:

1. Create a second key on the same collection with the same permission level.
2. Deploy the new key to all of your clients.
3. Once traffic on the old key drops to zero (visible in the dashboard under
   "Key activity"), revoke the old key.

Revoked keys stop working within 60 seconds. There is no way to un-revoke a
key; you must create a new one. Nimbus never shows a key's secret value again
after creation, so store it immediately in your own secret manager.

Keys never expire automatically. If a key is leaked, revoke it immediately from
the dashboard or with `nimbus keys revoke <key-id>`.
