# TECH-007 — Definir convenções de persistência JPA/Hibernate

## Objetivo

Preparar a camada de persistência JPA/Hibernate e documentar as convenções oficiais que serão usadas pelas futuras entidades e repositories.

## Dependências

- TECH-001
- TECH-002
- TECH-003
- TECH-004
- TECH-005
- TECH-006

## Documentação relacionada

- `AGENTS.md`
- `docs/architecture/persistence.md`
- `docs/architecture/concurrency.md`
- `docs/architecture/transactions.md`
- `docs/database/migrations.md`
- `docs/decisions/ADR-011-postgresql-and-database-naming.md`

## Incluído

- Spring Data JPA/Hibernate;
- schema validation;
- Domain separado de JpaEntity;
- Long + PostgreSQL identity;
- EnumType.STRING;
- BigDecimal + NUMERIC(19,2);
- LocalDate + DATE;
- Instant + TIMESTAMPTZ;
- optimistic locking convention;
- auditoria técnica;
- repository ports/adapters;
- camelCase PostgreSQL.

## Fora do escopo

- tabelas de negócio;
- entidades de negócio;
- repositories de negócio;
- endpoints;
- autenticação/autorização;
- Kafka;
- idempotência financeira.

## Critérios de aceite

- [ ] JPA configurado
- [ ] Flyway continua owner do schema
- [ ] Hibernate não altera schema
- [ ] Domain não recebe annotations JPA
- [ ] convenções documentadas
- [ ] camelCase preservado
- [ ] testes herméticos continuam sem Docker
- [ ] integration tests continuam compatíveis
- [ ] nenhuma entidade/tabela de negócio antecipada
