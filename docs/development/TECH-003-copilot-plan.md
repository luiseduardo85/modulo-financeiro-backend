# Copilot Plan — TECH-003

```text
Read .github/copilot-instructions.md first.

We are planning:
TECH-003 — Configure local PostgreSQL.

Read:
- docs/architecture/persistence.md
- docs/database/migrations.md
- docs/decisions/ADR-011-postgresql-and-database-naming.md
- docs/backlog/technical-backlog.md
- docs/backlog/issues/TECH-003-postgresql-local.md

Inspect the existing backend and current Maven dependencies.

Do not write code yet.

Goal:
Configure PostgreSQL for local development and allow the Spring Boot backend to connect to it.

Scope:
- PostgreSQL JDBC Driver;
- Docker Compose PostgreSQL service;
- local datasource configuration;
- connection settings through environment variables;
- persistent local Docker volume;
- validation that the application connects to PostgreSQL.

Database conventions:
- PostgreSQL is the official database;
- database identifiers use camelCase;
- do not introduce snake_case;
- quoted identifiers will be used when necessary to preserve camelCase.

Explicitly out of scope:
- Flyway;
- database migrations;
- business tables;
- JPA domain entities;
- repositories;
- Testcontainers;
- authentication;
- authorization;
- Kafka;
- seed business data.

Restrictions:
- do not use H2;
- do not use ddl-auto=update;
- do not allow Hibernate to create the business schema;
- do not commit real credentials.

Create a plan containing:
1. current dependency/configuration analysis;
2. dependencies to add or change;
3. Docker Compose changes;
4. environment variables;
5. Spring profile/configuration changes;
6. how database connectivity will be validated;
7. files to create or modify;
8. commands to run;
9. risks or conflicts with documentation.

Prefer the minimum change required for TECH-003.
Do not implement anything yet.
```
