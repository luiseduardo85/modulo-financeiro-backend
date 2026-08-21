# TECH-004 — Configurar Flyway

## Objetivo
Configurar Flyway como mecanismo oficial de evolução do schema PostgreSQL.

## Dependências
- TECH-001
- TECH-002
- TECH-003

## Documentação
- `.github/copilot-instructions.md`
- `docs/architecture/persistence.md`
- `docs/database/migrations.md`
- `docs/decisions/ADR-011-postgresql-and-database-naming.md`

## Incluído
- dependências Flyway necessárias;
- integração com PostgreSQL local;
- diretório de migrations;
- convenção de versionamento;
- validação de execução e reexecução.

## Fora do escopo
- tabelas de negócio;
- JPA/Hibernate;
- repositories;
- Testcontainers;
- autenticação/autorização;
- Kafka;
- seed de negócio.

## Critérios de aceite
- [ ] Flyway configurado
- [ ] PostgreSQL suportado
- [ ] `db/migration` criado
- [ ] execução local validada
- [ ] histórico do Flyway criado
- [ ] reinício idempotente validado
- [ ] nenhuma tabela de domínio criada
- [ ] JPA/Hibernate não introduzido
- [ ] Testcontainers não introduzido
- [ ] `mvn test` passa
- [ ] `mvn package` passa
