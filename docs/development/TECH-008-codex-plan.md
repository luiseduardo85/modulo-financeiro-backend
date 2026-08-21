# Codex Plan — TECH-008

```text
Read AGENTS.md first.

We are planning:

TECH-008 — Define backend testing conventions.

Before proposing changes, read:

- docs/architecture/testing.md
- docs/architecture/backend-architecture.md
- docs/architecture/persistence.md
- docs/architecture/transactions.md
- docs/development/definition-of-done.md
- docs/backlog/technical-backlog.md
- docs/backlog/issues/TECH-008-testing-conventions.md

Inspect the current backend after TECH-007.

Do not modify files yet.

Goal:
Consolidate the official backend testing strategy before business-domain implementation begins.

The strategy must clearly separate:

1. Domain unit tests
2. Application/use-case tests
3. REST/API tests
4. Persistence integration tests
5. Future E2E tests

Current execution contract:

mvn test
- Surefire
- hermetic tests
- no Docker
- no PostgreSQL local
- no .env

mvn verify
- includes Failsafe integration tests
- *IT naming
- PostgreSQL 16 Testcontainers
- real Flyway migrations
- real JPA/Hibernate where relevant

Required principles:

Domain tests:
- no Spring;
- no database;
- no Docker;
- test business behavior/invariants.

Application tests:
- preferably no Spring;
- instantiate use cases directly;
- replace external ports with focused fakes/stubs/mocks;
- avoid mocking domain internals.

REST tests:
- test HTTP contract, JSON, validation and adapter behavior;
- prefer focused tests;
- do not use @SpringBootTest automatically for every controller test.

Persistence integration tests:
- PostgreSQL 16 Testcontainers;
- Flyway migrations;
- JPA/Hibernate;
- no H2;
- no local PostgreSQL.

Naming:
- hermetic/unit/application/API tests: *Test
- infrastructure integration tests: *IT

Mocks:
- use only at architectural boundaries where useful;
- do not create a mock-heavy testing style;
- do not mock JPA/PostgreSQL/Flyway in persistence ITs.

Fixtures:
- keep test data close to the business context;
- do not introduce a global TestDataFactory/Mother/framework yet.

Time:
- avoid sleeps;
- use a testable time source when actual business code requires it;
- do not introduce unused Clock abstractions.

Coverage:
- do not define a mandatory percentage;
- do not add JaCoCo solely for TECH-008.

Explicitly out of scope:
- business entities;
- business Use Cases;
- E2E implementation;
- performance/load tests;
- external contract tests;
- authentication tests;
- CI/CD pipeline;
- mutation testing;
- global test-data framework.

Do not create:
- AbstractIntegrationTest unless there is an existing concrete need;
- generic test superclass;
- fake business entities;
- generic fixture framework;
- new business behavior.

Create a plan containing:

1. Current test-state analysis
   - existing tests;
   - Surefire/Failsafe;
   - Spring Boot testing dependencies;
   - Testcontainers;
   - current naming conventions.

2. Test taxonomy
   - exact definition and responsibility of each test level.

3. Maven execution model
   - what runs under test;
   - what runs under verify;
   - how Docker dependency remains isolated.

4. Domain-test convention

5. Application-test convention

6. REST/API-test convention
   - analyze the current Spring Boot 4.1.1 test support;
   - prefer the smallest focused configuration;
   - do not introduce additional dependencies without necessity.

7. Persistence-integration convention

8. Mock/fake/stub policy

9. Fixture/test-data policy

10. Time/Clock policy

11. Package and naming convention

12. Existing tests
   - identify whether any current TECH-005/006/007 test should be renamed/reorganized;
   - avoid churn unless there is a real inconsistency.

13. Files
   - exact files expected to modify/create.

14. Dependency/plugin changes
   - preferably none;
   - if a change is proposed, justify it concretely.

15. Validation commands
   - mvn test
   - mvn package
   - mvn verify when Docker is available
   - git diff --check

16. Scope compliance
   Confirm:
   - no business implementation;
   - no H2;
   - no E2E;
   - no JaCoCo requirement;
   - no generic test framework;
   - no authentication;
   - no CI/CD expansion.

17. Risks and assumptions
   - Spring Boot 4.1.1 test APIs;
   - test naming/plugin behavior;
   - Docker availability;
   - existing test organization.

Prefer documentation and small corrections over creating new infrastructure.

Do not implement yet.
```
