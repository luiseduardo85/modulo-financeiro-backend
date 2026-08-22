# AGENTS.md — Financeiro

## Purpose

This repository contains a multi-company financial management system.

Codex must use the repository documentation as the source of truth and must not invent business behavior.

## Technology

### Backend
- Java 21
- Spring Boot
- Maven
- PostgreSQL 16
- Flyway

### Frontend
- React
- TypeScript
- Tailwind CSS

### Testing
- JUnit
- Maven Surefire for hermetic/unit tests
- Maven Failsafe for integration tests
- PostgreSQL Testcontainers for persistence integration tests

Java changes must be Spotless compliant. Run `./mvnw spotless:apply` (or
`.\mvnw.cmd spotless:apply` on Windows) before final validation when Java files
change.

Authentication is provided by an external service and is intentionally pending.

Do not implement authentication until `docs/architecture/authentication.md` is completed.

---

## Source of Truth

Use this precedence when information conflicts:

1. Accepted ADRs
2. Business Rules
3. Domain Model
4. Use Cases
5. API Contracts
6. Existing code

Read documentation from:

- Business rules: `docs/requirements/business-rules.md`
- Permissions: `docs/requirements/permissions.md`
- Domain: `docs/domain/`
- Use cases: `docs/use-cases/`
- Architecture: `docs/architecture/`
- Database: `docs/database/`
- API: `docs/api/`
- ADRs: `docs/decisions/`
- Backlog: `docs/backlog/`

If code conflicts with documentation, report the conflict. Do not silently treat the code as correct.

---

## Business Rules

Never invent business rules.

If required behavior is missing or ambiguous:

1. identify the missing decision;
2. explain why it blocks or affects the requested implementation;
3. do not implement speculative behavior;
4. report the decision that must be made.

Do not add:
- new account statuses;
- new permissions;
- approval levels;
- database relationships;
- automatic financial behavior;

unless they are documented.

---

## Architecture

The backend follows Clean Architecture with pragmatic DDD.

Dependency direction:

- Domain depends on no framework or outer layer.
- Application may depend on Domain.
- Interfaces acts as an input adapter and should primarily use Application.
- Infrastructure implements ports required by inner layers and may depend on Domain/Application abstractions.

Domain must not depend on:
- Spring
- JPA
- Hibernate
- PostgreSQL
- Flyway
- HTTP
- Kafka
- authentication providers

Controllers must not contain business rules.

Do not create speculative generic abstractions such as:
- BaseEntity
- BaseRepository
- BaseService
- BaseController
- GenericCrudService
- GenericMapper

unless there is a concrete, documented need.

---

## Domain

Financial account lifecycle is controlled by domain behavior.

Prefer explicit domain transition methods such as `approve()` and `reject()`.

Do not change lifecycle through generic status setters.

Before modifying `FinancialAccount` lifecycle, read:

- `docs/domain/conta-financeira.md`
- `docs/domain/state-machine.md`
- `docs/requirements/business-rules.md`

Persisted statuses are:

- DRAFT
- PENDING_APPROVAL
- APPROVED
- SETTLED
- CANCELLED

Do not introduce other persisted main statuses without a documented decision.

---

## PostgreSQL

Official database:
- PostgreSQL 16

Schema evolution:
- Flyway

Database identifiers use camelCase.

Examples:
- `"financialAccount"`
- `"companyId"`
- `"issueDate"`

Do not introduce snake_case.

Because PostgreSQL folds unquoted identifiers to lowercase, quote identifiers when necessary to preserve camelCase.

Flyway owns schema evolution.

Never use:
- `ddl-auto=update`
- `ddl-auto=create`
- `ddl-auto=create-drop`

Do not edit an already-applied Flyway migration.

Money:
- Java: `BigDecimal`
- PostgreSQL: `NUMERIC(19,2)`

Never use float/double for money.

---

## Multi-company

Company-scoped data must always respect company isolation.

Never trust a company identifier from an ordinary business payload as the authorization source.

Repository queries for company-scoped resources must include company context where applicable.

A branch referenced by a financial account must belong to the same company.

---

## Transactions and concurrency

Transactional boundaries belong to Application Use Cases.

Critical financial operations must be atomic.

Use optimistic locking where documented.

Concurrent modifications must not silently overwrite financial data.

---

## Testing

### Hermetic tests
`mvn test`

These tests must not require:
- Docker
- PostgreSQL local
- `.env`

### Integration tests
`mvn verify`

Persistence integration tests use:
- PostgreSQL 16 Testcontainers
- real Flyway migrations

Do not use H2 as a PostgreSQL substitute.

Do not duplicate production migrations under test resources.

---

## Task Workflow

For every TECH or UC:

1. Read this `AGENTS.md`.
2. Read the task/Issue.
3. Read all referenced documentation.
4. Inspect current code.
5. Plan before changing files when the task is non-trivial.
6. Keep changes inside task scope.
7. Implement.
8. Run applicable checks.
9. Review the diff.
10. Report files changed, validations run, failures and unresolved decisions.

Prompts/tasks should be treated like GitHub Issues:
- objective;
- scope;
- out of scope;
- acceptance criteria;
- relevant paths/docs.

---

## Scope Control

Implement only the requested task and its direct dependencies.

Do not:
- implement future TECHs;
- implement future UCs;
- perform unrelated refactors;
- add dependencies "for later";
- create future package trees;
- add authentication before its contract is defined.

Prefer a smaller correct patch over speculative scaffolding.

---

## Validation

Before marking work complete:

1. inspect `git diff`;
2. run the task-specific commands;
3. run `mvn test` when backend code/configuration is affected;
4. run `mvn verify` when integration-test infrastructure or persistence behavior is affected and Docker is available;
5. run `mvn package` when appropriate;
6. validate Flyway migrations when database changes exist;
7. report any command that could not be executed and why.

Never claim a validation passed if it was not executed.

---

## Review Format

When explicitly asked to review a task, do not modify files unless requested.

Classify findings:

- CRITICAL
- HIGH
- MEDIUM
- LOW

For every finding include:
- file/path;
- issue;
- why it matters;
- recommended correction.

Finish with:

`REVIEW RESULT: APPROVED`

or

`REVIEW RESULT: CHANGES REQUIRED`
