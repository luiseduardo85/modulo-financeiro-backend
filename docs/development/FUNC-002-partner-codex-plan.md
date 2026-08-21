# Codex Plan — FUNC-002 Partner

```text
Read AGENTS.md first.

We are planning:

FUNC-002 — Partner.

Do not modify files yet.

Read repository documentation in precedence order, including:
- AGENTS.md
- docs/requirements/business-rules.md
- docs/domain/*
- docs/use-cases/*
- docs/api/*
- docs/database/*
- docs/architecture/backend-architecture.md
- docs/architecture/persistence.md
- docs/architecture/transactions.md
- docs/architecture/testing.md
- docs/backlog/issues/FUNC-002-partner.md

Inspect the backend after completed FUNC-001 Company/Branch and TECH-010.

Goal:
Design the minimum production-ready global Partner slice for future payable and
receivable financial operations.

Confirmed rules:
1. Partner is global.
2. Partner does NOT belong to Company.
3. Do NOT introduce PartnerCompany / ParceiroEmpresa.
4. Partner document is CPF or CNPJ.
5. Document must be globally unique.
6. Document must be validated.
7. Partner may have CUSTOMER role, SUPPLIER role, or both.
8. Partner becomes available immediately after creation.
9. Inactive Partner cannot be used in future new financial launches.
10. Existing historical references must remain valid.
11. Code/API/database/enums use English.
12. User-facing messages may be Portuguese.
13. External fiscal validation/integration is future scope.

Analyze:

1. Minimum domain model
Determine the smallest usable Partner model.
At minimum evaluate:
- id
- name
- document
- roles
- active

For every field, explain why it is required now.

Do not automatically add:
- trade name;
- legal name separate from display name;
- address;
- phone;
- email;
- state registration;
- municipal registration;
- bank data;
- company relation;
- audit-user fields;
- timestamps unless a concrete requirement requires them;
- metadata.

If name is required for a usable Partner, propose and justify its validation
contract. Do not invent multiple name fields.

2. Document Value Object
Design a framework-free Domain value object, e.g. Document.

It must distinguish CPF and CNPJ where necessary.

Analyze:
- input normalization;
- whether punctuation is accepted at API boundary;
- canonical stored representation;
- CPF length;
- CNPJ length;
- check-digit validation;
- repeated-digit invalid cases;
- null/blank behavior.

Prefer canonical persistence as digits only.

Do not call external services.
Do not place CPF/CNPJ algorithm in controllers or JPA entities.
Do not add third-party validation dependency if the JDK/domain implementation is
small and clear.

3. Document type
Determine whether Document contains/derives CPF/CNPJ type or whether a separate
persisted documentType column is beneficial.
Avoid duplicated source of truth if type is fully derivable from canonical
document length.

4. Roles
Partner can be:
- CUSTOMER
- SUPPLIER
- both.

Choose the smallest domain representation.
Evaluate Set<PartnerRole>, two booleans, or another explicit model.

Requirements:
- at least one role;
- no duplicates;
- stable English enum values;
- persistence must support both roles safely.

Do not introduce a generic role/permission subsystem.

5. Active/inactive
The inactive rule is confirmed.

Determine the smallest model:
- boolean active preferred unless lifecycle needs more.

Creation should make Partner active immediately.

Analyze whether FUNC-002 should implement:
- deactivate endpoint/use case;
- reactivate endpoint/use case.

Do not invent delete/soft-delete semantics.
Physical deletion should not be introduced.

6. Invariants
Define exact Domain invariants for:
- ID;
- name if approved;
- Document;
- roles;
- active state transitions if implemented.

Document uniqueness is a repository/database invariant, not a purely local
entity invariant.

7. Repository port
Define PartnerRepository with only operations required by approved use cases.

Likely needs:
- save;
- findById;
- findByDocument;
- paginated list.

Avoid redundant methods if one operation can safely serve the use case.
No generic BaseRepository.

8. Use cases
Evaluate minimum coherent set:
- CreatePartner
- GetPartner
- ListPartners

If activation lifecycle is approved:
- DeactivatePartner
- ReactivatePartner

Do not add:
- update general partner data;
- delete;
- company assignment;
- financial behavior.

For create:
- normalize and validate document;
- require role(s);
- enforce global uniqueness;
- rely on a real database unique constraint as final authority;
- map duplicate race safely to a stable conflict.

Do not rely only on exists-then-insert for correctness.

9. Persistence
Use separate JPA Infrastructure entity.
PostgreSQL 16 + Flyway.
Use quoted camelCase identifiers.

Determine exact table/columns.

Document uniqueness must be enforced with a real PostgreSQL UNIQUE constraint.
Do not add Company FK.
Do not use ordinal enums.
Do not use broad cascade.

10. Migration
V1-V4 are finalized and must remain unchanged.
Plan the next Flyway migration(s), beginning with V5.

Specify exact:
- table;
- columns;
- PK;
- unique constraints;
- check constraints;
- indexes only if justified.

If document type is derived, do not persist it redundantly unless justified.

11. API
Use English routes under:
/api/v1/partners

Plan minimum endpoints.

Likely:
POST /api/v1/partners
GET  /api/v1/partners/{id}
GET  /api/v1/partners

If lifecycle included, prefer explicit actions:
POST /api/v1/partners/{id}/deactivate
POST /api/v1/partners/{id}/reactivate

Do not expose JPA entities.

Define exact request/response shapes.

Evaluate whether API accepts formatted CPF/CNPJ and returns canonical or
formatted representation. Prefer one explicit stable contract.

12. Error behavior
Reuse TECH-006 error contract.

Plan stable codes for at least:
- PARTNER_NOT_FOUND
- PARTNER_DOCUMENT_ALREADY_EXISTS
- INVALID_PARTNER_DOCUMENT
- VALIDATION_ERROR as appropriate.

Keep layering clean and do not expose SQL/internal details.

13. Pagination/filtering
Reuse approved pagination:
- page default 0
- size default 20
- max 100
- explicit sort whitelist

Determine minimum sortable fields.

Do not add speculative search/filter functionality unless already documented.

14. Multi-company implications
Partner is global.

Confirm:
- no companyId column;
- no nested Company route;
- no company-scoped unique document;
- no PartnerCompany association.

15. Historical preservation
Explain how inactive Partner remains loadable by ID for historical data while
future FinancialAccount creation will later reject inactive Partner usage.

Do not implement FinancialAccount now.

16. Tests
Hermetic domain tests:
- CPF valid/invalid;
- CNPJ valid/invalid;
- repeated digits;
- normalization;
- roles;
- creation invariants;
- active lifecycle if included.

Application tests:
- create;
- duplicate document;
- get/not found;
- list;
- lifecycle if included.

REST tests:
- request/response contract;
- formatted/unformatted document behavior;
- invalid document;
- duplicate conflict;
- unknown fields;
- pagination;
- stable errors;
- traceId.

Persistence IT:
- PostgreSQL 16 Testcontainers;
- real Flyway V1-V5+;
- Hibernate validate;
- global document unique constraint;
- duplicate race/constraint translation where meaningful;
- role persistence;
- active persistence;
- pagination/sorting.

Keep:
mvn test = Docker-free
mvn verify = integration/PostgreSQL.

17. Concurrency
Document uniqueness must be safe under concurrent creates.
The database UNIQUE constraint is the final source of truth.
Do not introduce distributed locks, Redis, synchronized, or idempotency for
ordinary Partner creation.

18. TECH-010
Do not use FinancialIdempotencyService or Idempotency-Key for Partner CRUD.

19. Exact scope exclusions
Confirm no implementation for:
- Company/Branch changes beyond necessary references in docs;
- PartnerCompany;
- FinancialAccount;
- Installment;
- FinancialTransaction;
- approval;
- settlement;
- reversal;
- auth;
- permissions;
- external fiscal APIs;
- Kafka;
- Redis;
- outbox;
- import/export.

20. Files
List exact files expected to create/modify.

21. Validation
Plan:
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd verify
git diff --check
git status --short

22. Documentation conflicts
Identify any conflict between current Portuguese historical terms and the
approved English technical naming.
Do not silently override higher-precedence business rules.

This is PLAN ONLY.
Do not modify files yet.
```
