# TECH-008 - Define backend testing conventions

## Objective

Consolidate the official backend testing strategy before business-domain and
use-case implementation begins.

## Dependencies

- TECH-001 through TECH-007.

## Included

- Domain unit-test conventions;
- Application/use-case test conventions;
- focused REST/API test conventions;
- PostgreSQL persistence integration-test conventions;
- future E2E classification without implementation;
- `*Test` and `*IT` naming;
- Surefire and Failsafe execution contract;
- mock, fake, stub, fixture, package, and time policies;
- alignment of existing tests with the conventions.

## Execution contract

`mvn test` runs hermetic `*Test` classes through Surefire without Docker, a
database, `.env`, or external services.

`mvn verify` runs Surefire and Failsafe `*IT` classes. Persistence integration
tests use PostgreSQL 16 Testcontainers, production Flyway migrations, and real
JPA/Hibernate where relevant.

## Required conventions

- Domain tests use plain JUnit and no Spring.
- Application tests preferably instantiate use cases directly and replace only
  external ports with focused test doubles.
- REST tests prefer focused MVC slices and verify HTTP/JSON adapter contracts.
- Persistence integration tests never mock PostgreSQL, Flyway, JPA, or Hibernate.
- H2 is not an accepted PostgreSQL substitute.
- Test data stays close to its business context.
- Tests avoid arbitrary sleeps and use a testable time source only when real
  business behavior requires it.
- Coverage has no mandatory percentage and JaCoCo is not added by TECH-008.

## Out of scope

- Business entities and use cases;
- fake business models;
- E2E implementation;
- performance or load tests;
- external contract tests;
- authentication tests;
- CI/CD pipeline work;
- mutation testing;
- global fixture or test-data frameworks.

## Restrictions

Do not introduce:

- `AbstractIntegrationTest`;
- generic test superclasses;
- global `TestDataFactory` or Object Mother;
- generic fixture frameworks;
- unused Clock abstractions;
- Maven profiles or include/exclude rules without a demonstrated need.

## Acceptance criteria

- [ ] Testing taxonomy and conventions are documented.
- [ ] `mvn test` remains hermetic and Docker-free.
- [ ] `mvn verify` remains the `*IT` integration-test path.
- [ ] PostgreSQL 16 Testcontainers, Flyway, and JPA conventions are preserved.
- [ ] H2 remains prohibited.
- [ ] Existing REST tests use a focused MVC slice where supported.
- [ ] Existing test names follow `*Test` and `*IT`.
- [ ] No speculative testing framework or business behavior is created.
- [ ] Existing tests and package validation pass.
