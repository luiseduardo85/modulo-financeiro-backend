# Copilot Plan — TECH-004

```text
Read .github/copilot-instructions.md first.

We are planning:

TECH-004 — Configure Flyway.

Read:
- docs/architecture/persistence.md
- docs/database/migrations.md
- docs/decisions/ADR-011-postgresql-and-database-naming.md
- docs/backlog/technical-backlog.md
- docs/backlog/issues/TECH-004-flyway.md

Inspect the current backend after TECH-003.

Do not write code yet.

Goal:
Configure Flyway as the official PostgreSQL schema migration mechanism.

Scope:
- add Flyway dependencies required by the current Spring Boot version;
- configure Flyway against the existing local PostgreSQL datasource;
- create the official migration directory;
- define migration naming/versioning conventions;
- validate Flyway startup and migration history behavior;
- keep schema evolution owned exclusively by Flyway.

PostgreSQL:
- version 16;
- identifiers use camelCase;
- use quoted identifiers when needed to preserve camelCase.

Explicitly out of scope:
- business tables;
- JPA/Hibernate;
- repositories;
- Testcontainers;
- authentication;
- authorization;
- Kafka;
- business seed data;
- functional Use Cases.

Restrictions:
- do not use H2;
- do not add JPA only to test Flyway;
- do not create company/financial/domain tables;
- do not use Hibernate schema generation;
- do not introduce snake_case;
- do not edit already-applied migrations.

Create a plan containing:
1. current dependency/configuration analysis;
2. Flyway dependencies required;
3. configuration changes;
4. migration directory/files to create;
5. whether a technical V1 migration is actually necessary;
6. how Flyway execution will be validated locally;
7. how restart/idempotency will be validated;
8. files to create or modify;
9. commands to run;
10. risks, assumptions or documentation conflicts.

Prefer the minimum change necessary for TECH-004.

Do not implement anything yet.
```
