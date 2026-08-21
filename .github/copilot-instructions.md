# GitHub Copilot Instructions

## Project
Multi-company financial management system.

Backend: Java, Spring Boot, JPA/Hibernate, PostgreSQL, Flyway.
Frontend: React, TypeScript, Tailwind CSS.
Testing: JUnit and PostgreSQL Testcontainers.

Authentication is external and intentionally pending. Do not implement authentication until `docs/architecture/authentication.md` is completed.

## Source of Truth
Priority:
1. Accepted ADRs
2. Business Rules
3. Domain Model
4. Use Cases
5. API Contracts
6. Existing code

Read:
- `docs/requirements/business-rules.md`
- `docs/requirements/permissions.md`
- `docs/domain/`
- `docs/use-cases/`
- `docs/architecture/`
- `docs/database/`
- `docs/api/`
- `docs/decisions/`

If documentation conflicts with code, report the conflict. Never silently follow the code.

## Rules
- Never invent business rules.
- Implement only the requested Use Case and direct dependencies.
- Domain must not depend on Spring, JPA, Hibernate, PostgreSQL, HTTP, Kafka or authentication services.
- Controllers contain no business rules.
- Do not expose lifecycle changes through generic status setters.
- Read `docs/domain/state-machine.md` before changing ContaFinanceira lifecycle.
- PostgreSQL identifiers use camelCase; do not introduce snake_case.
- Quote camelCase PostgreSQL identifiers where necessary.
- Flyway owns schema evolution.
- Never use `ddl-auto=update`.
- Money uses `BigDecimal` / `NUMERIC(19,2)`.
- Do not use H2 as PostgreSQL substitute.
- Use PostgreSQL Testcontainers for persistence integration tests.
- Company-scoped data must be isolated by company.
- Do not trust companyId from ordinary business payloads.
- Transactional boundaries belong to Application Use Cases.
- Use optimistic locking where documented.
- Add tests for changed business behavior.
- Do not implement future functionality speculatively.

## Before Coding
1. Read the Use Case.
2. Read referenced Business Rules.
3. Read relevant Domain documentation.
4. Read relevant ADRs.
5. Inspect existing code.
6. Create an implementation plan.
7. Identify tests.
8. Report unclear or conflicting requirements.

## After Coding
1. Compile.
2. Run relevant unit tests.
3. Run relevant integration tests.
4. Validate Flyway migrations.
5. Verify architecture boundaries.
6. Verify business rules.
7. Report deviations or unresolved decisions.
