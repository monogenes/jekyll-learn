# Nimbus DB — Regions and Data Residency

Nimbus DB operates in three regions:

- **us-east** — Virginia, USA.
- **eu-west** — Dublin, Ireland. Certified for GDPR data residency.
- **ap-south** — Mumbai, India.

A collection is pinned to one region for its entire life. To "move" a
collection to another region you must create a new collection in the target
region and re-insert your documents; there is no in-place move.

Access keys are also region-local: a key created against a `us-east`
collection cannot authenticate against a collection in `eu-west`, even within
the same account.

Cross-region replication is available only on the Cumulus tier. When enabled,
writes to the primary region are copied to one secondary region within about
five seconds. Reads always go to the region you address; there is no automatic
routing to the nearest region.
