# TECH-003 — Configurar PostgreSQL local

## Objetivo

Configurar PostgreSQL como banco de dados local do backend utilizando Docker Compose e configurar o datasource Spring para o profile `local`.

A tarefa deve apenas estabelecer a infraestrutura de conexão local. Nenhuma tabela ou modelagem de negócio deve ser criada.

## Documentação relacionada

- `.github/copilot-instructions.md`
- `docs/architecture/persistence.md`
- `docs/database/migrations.md`
- `docs/decisions/ADR-011-postgresql-and-database-naming.md`
- `docs/backlog/technical-backlog.md`

## Dependências

- TECH-001
- TECH-002

## Escopo incluído

- PostgreSQL JDBC Driver
- Docker Compose para PostgreSQL
- datasource no profile `local`
- variáveis de ambiente para conexão
- volume local persistente
- validação de conexão
- documentação mínima de execução local

## Fora do escopo

- Flyway
- migrations
- tabelas
- JPA entities de domínio
- repositories
- Testcontainers
- autenticação
- autorização
- Kafka
- dados seed

## Restrições

- não usar H2;
- não usar `ddl-auto=update`;
- não permitir Hibernate criar schema de negócio;
- não criar tabelas de domínio;
- não introduzir snake_case;
- não versionar credenciais reais.

## Critérios de aceite

- [ ] PostgreSQL Driver configurado
- [ ] PostgreSQL sobe via Docker Compose
- [ ] datasource local configurado
- [ ] configuração sensível usa variáveis de ambiente
- [ ] backend conecta ao PostgreSQL
- [ ] aplicação inicia com profile local
- [ ] nenhuma tabela de negócio criada
- [ ] Flyway continua fora do escopo
- [ ] Testcontainers continua fora do escopo
- [ ] nenhuma autenticação criada
- [ ] `mvn test` passa
- [ ] `mvn package` passa
