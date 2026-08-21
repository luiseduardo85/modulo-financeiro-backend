# Backend Testing Strategy

Tests are separated by architectural purpose and infrastructure needs. Numeric
coverage is supporting evidence; it does not replace complete behavioral tests
for critical documented rules. TECH-008 defines no mandatory percentage.

## Maven execution contract

### Hermetic suite

`mvn test` uses Maven Surefire and runs classes named `*Test`.

These tests must not require:

- Docker;
- a local or containerized PostgreSQL instance;
- `.env`;
- external services.

Domain, Application, and focused REST/API tests belong to this suite.

### Integration suite

`mvn verify` runs the Surefire suite and Maven Failsafe integration tests named
`*IT`.

Persistence integration tests use:

- PostgreSQL 16 Testcontainers;
- production Flyway migrations;
- real JPA/Hibernate where relevant;
- no H2 and no developer-local PostgreSQL.

Do not duplicate production migrations under test resources. The existing
`PostgresIntegrationTestConfiguration` is imported through composition; do not
introduce an abstract integration-test superclass.

## Test taxonomy

### Domain unit tests

- Use plain JUnit without Spring.
- Instantiate aggregates, entities, and value objects directly.
- Test documented behavior, invariants, transitions, and boundary values.
- Use no database, Docker, HTTP, or environment configuration.
- Do not mock domain internals.

### Application and use-case tests

- Prefer direct construction of the use case without Spring.
- Replace external ports with focused fakes, stubs, or mocks.
- Test orchestration, port collaboration, and observable results.
- Use real domain objects instead of mocking domain behavior.
- Use a Spring context only when Spring wiring or transactional proxies are the
  behavior under test.

### REST and API tests

- Test routes, methods, status codes, JSON, validation, error contracts, and
  controller-to-application adapter behavior.
- Prefer the focused Spring MVC slice with `@WebMvcTest` and import only required
  MVC collaborators.
- Do not use `@SpringBootTest` automatically for controller tests.
- A full context is justified only when complete application wiring is relevant.
- Keep test-only controllers and DTOs under `src/test`.

### Persistence integration tests

- Use the `*IT` suffix and Maven Failsafe.
- Use PostgreSQL 16 Testcontainers, Flyway, and real JPA/Hibernate.
- Test mappings, constraints, queries, locking, and PostgreSQL-specific behavior.
- Do not mock JPA, Hibernate, JDBC, PostgreSQL, or Flyway.
- Future quoted camelCase mappings require integration tests against PostgreSQL.

### End-to-end tests

E2E tests are deferred. Future E2E coverage should contain only a small number
of critical cross-layer flows. Its execution environment and Maven lifecycle are
not defined by TECH-008.

## Test doubles

- Use test doubles only at architectural boundaries where they improve clarity.
- Use stubs for predetermined responses, small fakes for useful stateful behavior,
  and mocks when interaction verification is the actual requirement.
- Prefer result and state assertions over verifying incidental calls.
- Avoid deep stubs, mock-heavy tests, and in-memory reimplementations of production
  infrastructure.
- Do not mock aggregates, value objects, DTOs, or persistence infrastructure merely
  to avoid constructing or executing them.

## Fixtures and test data

- Keep data close to the test and its business context.
- Start with explicit values or private factory methods in the test class.
- Extract a context-specific builder only after concrete repetition exists.
- Do not introduce a global `TestDataFactory`, Object Mother, generic fixture
  framework, or fake business model in TECH-008.

## Time

- Do not use `Thread.sleep` or depend on wall-clock timing.
- When real business behavior requires time, inject a testable source such as
  `Clock` and use a fixed value in tests.
- Do not introduce an unused Clock wrapper, bean, or abstraction.
- Infrastructure tests may use bounded waiting supplied by the tested library,
  not arbitrary sleeps.

## Package and naming conventions

Tests mirror the production context and layer:

```text
src/main/java/com/financeiro/<context>/domain/...
src/test/java/com/financeiro/<context>/domain/...Test.java

src/main/java/com/financeiro/<context>/application/...
src/test/java/com/financeiro/<context>/application/...Test.java

src/main/java/com/financeiro/<context>/interfaces/rest/...
src/test/java/com/financeiro/<context>/interfaces/rest/...Test.java

src/main/java/com/financeiro/<context>/infrastructure/persistence/...
src/test/java/com/financeiro/<context>/infrastructure/persistence/...IT.java
```

Cross-cutting integration support may remain under
`com.financeiro.integration.support`.

Do not introduce generic test superclasses, `AbstractIntegrationTest`, or empty
future package trees.
