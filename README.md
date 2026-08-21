# Financeiro — Documentação v1.0

SaaS de gestão financeira multiempresa.

## Stack
- Backend: Java, Spring Boot, JPA/Hibernate
- Banco: PostgreSQL
- Migrations: Flyway
- Frontend: React, TypeScript, Tailwind CSS
- Testes: JUnit e PostgreSQL Testcontainers

## Autenticação
A autenticação será provida por um serviço externo. Sua integração ainda não está definida e não deve ser implementada até existir documentação específica.

## Local development

### PostgreSQL (Docker Compose)

1. Copy `.env.example` to `.env` and adjust values if needed (defaults are local-dev-only, not real
   credentials).
2. Start PostgreSQL:
   ```
   docker compose up -d
   ```
3. Wait for the container to report healthy (the compose healthcheck runs `pg_isready`):
   ```
   docker compose ps
   ```
4. Run the backend with the `local` profile (from `backend/`, with `JAVA_HOME` pointing to a JDK 21):
   ```
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```
5. What to check to confirm PostgreSQL connectivity at this stage. HikariCP acquires connections
   lazily, so a normal application startup will **not** necessarily print an explicit
   `HikariPool-1 - Start completed` / connection-acquired log line — that only happens once something
   actually requests a connection, which nothing in the codebase does yet. At this stage, validation
   consists of:
   - `docker compose ps` shows the `postgres` service as `healthy` (via `pg_isready`);
   - the application starts with the `local` profile with no `DataSource`/connection configuration
     errors in the logs.

   Automated connectivity validation (e.g. an actual query/health check exercising the datasource) will
   be introduced in TECH-005.
6. Stop the container (data persists in the named volume across restarts):
   ```
   docker compose down
   ```
   Use `docker compose down -v` only if you intentionally want to discard the local database volume.

### Flyway (database schema migrations)

Flyway runs automatically on backend startup (`local` profile) and manages schema evolution against
the PostgreSQL datasource configured above. It is the only mechanism responsible for schema changes —
Hibernate/JPA schema generation is not used.

- Migration files live in `backend/src/main/resources/db/migration/`.
- Naming convention: `V<version>__<description>.sql` (e.g. `V1__flyway_bootstrap.sql`,
  `V2__<description>.sql`). Versions are sequential integers; never edit an already-applied migration —
  create a new one instead.
- To validate Flyway:
  1. Start PostgreSQL and run the backend with the `local` profile as described above. On first
     startup, the logs show Flyway creating the schema history table and applying `V1`.
  2. Inspect the history table:
     ```
     docker exec -it <container> psql -U financeiro -d financeiro -c 'SELECT * FROM "flyway_schema_history";'
     ```
     Confirm `V1` is recorded with `success = true`.
  3. Stop and restart the application with the same profile. The logs should report no pending
     migrations on this second run, and `V1` must not be reapplied.

