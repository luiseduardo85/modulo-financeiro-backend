# Financial Idempotency

## HTTP contract

Future financial endpoints that require retry protection use the client-generated
`Idempotency-Key` request header. The value is opaque and case-sensitive. It must
contain between 1 and 128 visible ASCII characters (`!` through `~`). Values are
not trimmed or normalized, and the server never generates a replacement key.

Missing keys map to `IDEMPOTENCY_KEY_REQUIRED`; invalid keys map to
`INVALID_IDEMPOTENCY_KEY`. Both use HTTP 400.

## Scope and fingerprint

The PostgreSQL uniqueness scope is `companyId + operation + idempotencyKey`.
Target/resource identity is a material fingerprint component, not part of the
unique constraint. Company identity must come from trusted context when that
contract exists, never from an ordinary financial payload.

Fingerprints are lowercase SHA-256 hexadecimal strings built from ordered,
semantic command components. Each UTF-8 component is length-prefixed before
hashing. Future use cases must document their fixed component order, type-specific
normalization, target identity, and fingerprint schema version. Raw JSON and
complete request bodies are not fingerprint inputs.

## Transaction and concurrency contract

A future financial Application use case owns one transaction containing:

1. claim the idempotency key;
2. perform the financial database mutation;
3. store the result reference and complete the idempotency record;
4. commit once.

Claim and completion use transaction propagation `MANDATORY`. They must not use
`REQUIRES_NEW` or commit separately. PostgreSQL's unique constraint and
`INSERT ... ON CONFLICT DO NOTHING` are the concurrency source of truth.

The persisted technical states are only `PROCESSING` and `COMPLETED`. A failure
rolls the entire transaction back, including the claim, so no `FAILED` state is
stored. A same-key, same-fingerprint completed retry returns its result reference
without accepting another mutation. A different fingerprint maps to
`IDEMPOTENCY_KEY_CONFLICT` and HTTP 409.

This contract covers database-local atomic work. Non-transactional external side
effects require a separate future design.

## Result and retention

Only an opaque `resultReference` is stored. The future operation uses it to reload
its authoritative result; complete HTTP responses are not persisted.

Records are retained indefinitely for now. TTL, archival, cleanup, and scheduling
require a future operational decision because deletion can permit an old retry to
execute again.

## Scope

This infrastructure does not implement a financial operation, authentication,
authorization, Kafka, Redis, retries, outbox processing, or cleanup jobs. Domain
has no dependency on HTTP, idempotency, JPA, PostgreSQL, or logging infrastructure.
