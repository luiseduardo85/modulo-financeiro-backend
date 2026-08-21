# Codex Plan — Company / Branch

```text
Read AGENTS.md first.

Plan the first real functional/domain slice: Company / Branch.

Do not modify files yet.

Read, in documented precedence order:
- AGENTS.md
- docs/business-rules/*
- docs/domain/*
- docs/use-cases/*
- docs/architecture/backend-architecture.md
- docs/architecture/persistence.md
- docs/architecture/transactions.md
- docs/architecture/testing.md
- docs/api/conventions.md
- docs/api/errors.md
- docs/database/migrations.md
- docs/backlog/issues/FUNC-001-company-branch.md

Inspect the backend after TECH-010.

Goal:
Design the minimum production-ready Company/Branch slice establishing the
multi-company foundation without anticipating unrelated ERP functionality.

Confirmed constraints:
1. Multi-company system.
2. Branch belongs to exactly one Company.
3. Branch cannot be shared across Companies.
4. Future FinancialAccount requires Branch.
5. User/branch access is out of scope.
6. Authentication/authorization is deferred.
7. Code, API fields, enums and database identifiers are English.
8. User-facing messages may be Portuguese.

Analyze:

1. Domain model
Propose the minimum Company and Branch entities.
For every field explain why it is required now.
Do not automatically add tax identifiers, addresses, contacts, state
registration, audit-user fields, soft delete or generic metadata.
Use Long IDs.

2. Invariants
Define only justified invariants.
Branch must belong to Company.
Prevent cross-company Branch lookup.
Do not assume global name uniqueness without documentation.

3. Domain behavior
Keep Domain framework-free.
Avoid both speculative rich behavior and pure persistence-shaped modeling.

4. Repository ports
Define only ports needed by the selected use cases.
No BaseRepository or generic CRUD abstractions.

5. Application use cases
Evaluate the minimum set:
- CreateCompany
- GetCompany
- ListCompanies
- CreateBranch
- GetBranch
- ListBranchesByCompany

Do not add update/delete/deactivate automatically.
Application owns transactions.
No authorization yet.

6. Persistence
Separate JPA entities in Infrastructure.
Use PostgreSQL 16, Flyway, BIGINT identity, quoted camelCase, explicit
nullability and no broad CascadeType.ALL.
Branch should have a real FK to Company if both tables are created.
No cascading delete for convenience.

7. Migrations
Follow existing V1-V3 history.
Do not modify prior migrations.
Define exact next version(s), tables, PKs, FKs, constraints and only justified
indexes.

8. REST API
Use `/api/v1`.
Evaluate:
POST /api/v1/companies
GET /api/v1/companies/{id}
GET /api/v1/companies
POST /api/v1/companies/{companyId}/branches
GET /api/v1/companies/{companyId}/branches/{branchId}
GET /api/v1/companies/{companyId}/branches

Use English request/response field names.
Do not expose JPA entities.
Explicitly document that route companyId is not yet authenticated context.

9. Errors
Reuse TECH-006.
Plan stable errors for company not found, branch not found, invalid requests,
cross-company lookup, and any justified uniqueness conflict.
No internal/SQL leakage.

10. Pagination
Reuse existing page=0,size=20,max=100 and sort whitelist conventions if already
documented.

11. Tests
Domain/Application tests: Docker-free.
REST: focused MVC.
Persistence IT: PostgreSQL 16 Testcontainers + real Flyway/Hibernate/JPA.
Cover FK enforcement, quoted camelCase mapping, repository behavior and
cross-company isolation.
Keep `mvn test` hermetic and `mvn verify` for integration tests.

12. TECH-010
Do not apply Idempotency-Key to Company/Branch CRUD merely because the
infrastructure exists.

13. Explicitly exclude
User, Profile, Permission, authentication, authorization, Partner, Category,
CostCenter, BankAccount, PaymentMethod, FinancialAccount, Installment,
FinancialTransaction, Approval, Settlement, Reversal, Kafka, Redis and outbox.

14. List exact files expected to create/modify.

15. Validation
Plan:
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd verify
git diff --check

16. Identify documentation conflicts and do not silently resolve them against
the repository precedence rules.

This is PLAN ONLY. Do not modify files yet.
```
