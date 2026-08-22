# Codex Plan — FUNC-003 Category / Cost Center + Spotless

```text
Read AGENTS.md first.

We are planning:
FUNC-003 — Category / Cost Center

Plus one approved technical cross-cutting addition:
Global Java formatting with Spotless.

Do not modify files yet.

Read:
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
- docs/backlog/issues/FUNC-003-category-cost-center.md

Inspect the backend after completed FUNC-001, FUNC-002 and TECH-010.

Confirmed:
- Category belongs to exactly one Company.
- CostCenter belongs to exactly one Company.
- Both are shared between PAYABLE and RECEIVABLE.
- Do not add a payable/receivable type field.
- Inactive records cannot be selected by future new FinancialAccount creation.
- Historical references remain valid.
- Auth is deferred.
- companyId in routes is resource scope only.
- Technical naming is English.

1. Domain models
Evaluate minimum:
Category: id, companyId, name, active
CostCenter: id, companyId, name, active

For each field justify why needed now.

Do not add code, description, hierarchy, accounting fields, timestamps, version,
soft delete or metadata without a current requirement.

Name contract should preferably match existing Company/Branch/Partner:
required, String.strip(), nonblank, max 200, normalized persistence.
Do not infer uniqueness.

2. Ownership
Use immutable scalar Long companyId in Domain and JPA.
Do not introduce @ManyToOne solely for ownership.
Use real PostgreSQL FK to "company"("id").
All application/API lookups must be companyId + resourceId scoped.
Cross-company access -> same resource NOT_FOUND.

3. Lifecycle
Prefer boolean active.
Creation active=true.
Evaluate DeactivateCategory and DeactivateCostCenter.
Do not implement reactivation unless higher-precedence docs explicitly authorize it.
Repeated deactivation may be monotonic/idempotent.
No physical delete.

4. Repositories
Specific ports only:
CategoryRepository
CostCenterRepository
Likely save, findByCompanyIdAndId, findPageByCompanyId.

Create/list need Company existence validation.
Evaluate reuse of existing CompanyRepository versus a narrower company-existence
port; choose smallest design without duplicating concepts.

No generic repositories or Spring Data types inward.

5. Use cases
Evaluate:
CreateCategory
GetCategory
ListCategoriesByCompany
DeactivateCategory

CreateCostCenter
GetCostCenter
ListCostCentersByCompany
DeactivateCostCenter

Create: Company must exist, companyId comes from route/use-case input, body must
not own companyId.
Get/deactivate: direct scoped lookup by companyId + id.
List: verify Company exists; existing company with no records -> empty page;
include active and inactive.

6. Persistence
Separate CategoryJpaEntity and CostCenterJpaEntity.
Fields only: id, companyId, name, active.
BIGINT identity, VARCHAR(200), BOOLEAN.
quoted camelCase.
No @ManyToOne, cascade, timestamps, version, BaseEntity.

7. Migration
V1-V5 immutable.
Plan V6, preferably one migration for both tables.

Likely:
"category" (id, companyId, name, active)
"costCenter" (id, companyId, name, active)

Real FKs to "company".
BTRIM name checks.
Indexes on companyId may be justified by every scoped list/lookup; explain.
No name uniqueness unless documented.
No ON DELETE CASCADE.
No type/code/hierarchy columns.

8. REST
Evaluate:
POST /api/v1/companies/{companyId}/categories
GET  /api/v1/companies/{companyId}/categories
GET  /api/v1/companies/{companyId}/categories/{categoryId}
POST /api/v1/companies/{companyId}/categories/{categoryId}/deactivate

POST /api/v1/companies/{companyId}/cost-centers
GET  /api/v1/companies/{companyId}/cost-centers
GET  /api/v1/companies/{companyId}/cost-centers/{costCenterId}
POST /api/v1/companies/{companyId}/cost-centers/{costCenterId}/deactivate

Request only name unless another field is approved.
Reject id/companyId/active/unknown fields locally at DTO boundary.
201 + Location on create; 200 otherwise.
No JPA entities in REST.

9. Errors
Reuse existing contract.
Plan:
CATEGORY_NOT_FOUND -> 404
COST_CENTER_NOT_FOUND -> 404
COMPANY_NOT_FOUND -> 404
VALIDATION_ERROR -> 422
MALFORMED_REQUEST -> 400
INTERNAL_ERROR -> 500

No duplicate-name conflict unless uniqueness is approved.

10. Pagination
Reuse page=0, size=20, min 1, max 100, default id,asc.
Likely whitelist only id and name.
Do not add active sorting/filter or search without requirement.
Use deterministic id tie-breaker for equal names if appropriate.

11. Historical behavior
Inactive records remain retrievable/listable.
Future FinancialAccount creation will reject inactive references, but do not
implement FinancialAccount now.

12. Tests
mvn test Docker-free:
- Domain invariants, normalization, active/deactivate/rehydrate.
- Application company existence, scoped isolation, lists, empty lists, inactive,
  repeated deactivate.
- Focused MVC all routes, 201 Location, strict unknown fields, pagination/sort,
  stable errors, traceId, no Idempotency-Key.

mvn verify:
- PostgreSQL 16 Testcontainers;
- Flyway V1-V6;
- Hibernate validation;
- identities;
- real Company FKs;
- nonexistent company rejected by DB;
- no cascade delete;
- scoped isolation/listing;
- duplicate-name behavior;
- active/inactive persistence;
- pagination and all approved sorts.

13. Spotless
Analyze current pom.xml and add the smallest stable Maven Spotless setup.

Requirements:
- one reproducible Java formatter;
- `.\mvnw.cmd spotless:apply`;
- `.\mvnw.cmd spotless:check`;
- committed configuration;
- future Codex work must be formatter compliant.

Prefer google-java-format if compatible with current Java 21/build constraints,
unless repository facts justify another direct Spotless formatter.

Do not add Checkstyle, PMD, Sonar, IDE-only formatter XML or unrelated linting.

Determine whether spotless:check should bind to a Maven lifecycle phase.
Prefer normal CI/build validation to reject unformatted Java, while apply remains
an explicit repair command.

14. Existing-code formatting
The repository has Java files with poor spacing/line breaks.

Plan to run Spotless across current Java main/test sources once.
Formatting-only changes must not change behavior.

Do not format migrations or unrelated files.
Do not perform semantic refactors while formatting.
If the diff is large, explicitly recommend a separate formatting commit even
though configuration is introduced during FUNC-003.

15. Documentation / AGENTS
Document the global formatter convention and developer commands.
If appropriate, add a concise durable rule to AGENTS.md:
Java changes must be Spotless compliant; apply formatter before final validation.

Do not overload AGENTS.md with plugin internals.

16. Final validation order
Prefer:
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd verify
git diff --check

Explain lifecycle redundancy if spotless:check is bound automatically.

17. Scope exclusions
No Category/CostCenter hierarchy, accounting codes, budgets/rateio,
payable/receivable-specific ownership, Company/Branch/Partner feature changes,
BankAccount, PaymentMethod, FinancialAccount, auth, Kafka, Redis, outbox.

18. Files
List exact files to create/modify, separated into:
A. functional files;
B. Spotless/config/docs;
C. existing Java files changed only by formatting.

19. Documentation conflicts
Identify conflicts about global vs company-scoped Category/CostCenter, Portuguese
vs English technical names, uniqueness, code fields, hierarchy and lifecycle.
Respect source precedence.

This is PLAN ONLY. Do not modify files yet.
```
