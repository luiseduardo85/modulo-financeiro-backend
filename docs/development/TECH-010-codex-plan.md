# Codex Plan — TECH-010

```text
Read AGENTS.md first.

We are planning:

TECH-010 — Define financial idempotency infrastructure.

Before proposing changes, read:

- docs/architecture/backend-architecture.md
- docs/architecture/persistence.md
- docs/architecture/transactions.md
- docs/architecture/concurrency.md
- docs/api/conventions.md
- docs/api/errors.md
- docs/database/migrations.md
- docs/backlog/technical-backlog.md
- docs/backlog/issues/TECH-010-financial-idempotency.md

Inspect the backend after TECH-009.

Do not modify files yet.

Goal:
Design the minimum PostgreSQL-backed idempotency infrastructure required before
implementing settlement and reversal operations.

Important:
TECH-010 must NOT implement any real financial operation.

Analyze:

1. Idempotency-Key contract
- validate whether `Idempotency-Key` should be the official header;
- define blank/invalid handling;
- define maximum length;
- treat it as opaque;
- do not generate a new server key for client retry semantics.

2. Uniqueness scope
At minimum evaluate:
companyId + operation + idempotencyKey

Determine whether target/resource identity must also participate.

Multi-company isolation is mandatory.

3. Fingerprint
Same key reused for a materially different logical command must conflict.

Propose the smallest deterministic strategy.

Do not:
- store full request bodies by default;
- hash raw JSON whose field order may vary;
- introduce canonical-JSON infrastructure without a concrete need.

4. Persistence model
Evaluate a technical table such as `"idempotencyRecord"`.

Propose only fields actually required, considering concepts such as:
- id
- companyId
- operation
- idempotencyKey
- fingerprint
- status
- resource/result reference
- createdAt
- completedAt

Use PostgreSQL 16, Flyway and quoted camelCase identifiers.

JPA entities belong only to Infrastructure.

5. Status model
Evaluate whether PROCESSING / COMPLETED / FAILED are actually necessary.

Avoid a complex state machine.

These are technical states, not ContaFinanceira statuses.

6. Concurrency
Two simultaneous requests with the same scoped key must produce at most one accepted execution.

Correctness must rely on PostgreSQL mechanisms such as:
- unique constraint;
- transaction;
- constraint-conflict handling;
- locking only if actually necessary.

Do not use:
- synchronized;
- ConcurrentHashMap;
- local cache;
- process memory as source of truth.

7. Retry result
For a completed same-key/same-fingerprint retry, determine the minimum persisted
result/reference needed so a future use case can return an equivalent result
without executing the mutation again.

Do not persist the entire HTTP response without a concrete need.

8. Conflict behavior
Same scoped key with a different fingerprint should map to:
- HTTP 409;
- stable technical error code.

Integrate with the existing TECH-006 error infrastructure without creating a
financial business exception.

9. Transactions
Document how future financial use cases should coordinate atomically:

claim idempotency key
+
financial mutation
+
complete idempotency record

Analyze failure cases carefully.

Avoid designs where the financial mutation commits but idempotency state does
not, or the reverse.

10. Architecture
Do not make Domain depend on:
- Idempotency-Key HTTP header;
- servlet APIs;
- JPA;
- PostgreSQL.

Determine the smallest Application port/service + Infrastructure adapter design.

Do not build a generic idempotency framework for every command in the system.

11. Migration
If persistence is required, define the exact Flyway migration:
- table;
- columns;
- PK;
- unique constraint;
- indexes;
- column types;
- camelCase quoted identifiers.

Do not add financial/business tables.

12. Testing
Hermetic logic tests remain under `mvn test`.

Database/concurrency tests must be `*IT` under `mvn verify`, using:
- PostgreSQL 16 Testcontainers;
- real Flyway;
- real JPA/Hibernate.

Cover conceptually:
- first claim succeeds;
- same key/fingerprint is recognized;
- same key/different fingerprint conflicts;
- same key across companies is isolated;
- operation scope behaves as defined;
- concurrent duplicate attempts yield a single claim;
- JPA mapping matches the quoted camelCase schema.

13. Retention
Do not implement TTL, cleanup job or scheduler.
Document retention as a future operational decision.

14. Files
List exact files to create/modify.

15. Dependencies
Prefer no new dependency.
Do not add Redis, cache libraries, messaging, or hashing libraries if JDK capabilities suffice.

16. Validation
Plan:
- mvn test
- mvn package
- mvn verify with Docker
- git diff --check

17. Scope compliance
Explicitly confirm no:
- ContaFinanceira;
- Parcela;
- MovimentacaoFinanceira;
- settlement;
- payment;
- receipt;
- reversal;
- authentication;
- authorization;
- Kafka;
- Redis;
- retry engine;
- outbox;
- cleanup scheduler.

18. Risks and unresolved decisions
Identify:
- transaction race conditions;
- PostgreSQL constraint behavior;
- retry/failure semantics;
- fingerprint stability;
- future result reconstruction;
- multi-company scope;
- documentation conflicts.

Prefer the smallest correct infrastructure usable later by settlement and reversal.

Do not implement yet.
```
