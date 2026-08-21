# TECH-005 — Configurar PostgreSQL Testcontainers

## Objetivo
Criar a infraestrutura oficial de testes de integração de persistência usando PostgreSQL 16 via Testcontainers,
provando que Flyway executa as migrations reais e que o `ApplicationContext` inicializa corretamente contra um
banco real.

## Dependências
- TECH-001
- TECH-002
- TECH-003
- TECH-004

## Documentação
- `.github/copilot-instructions.md`
- `docs/architecture/persistence.md`
- `docs/architecture/testing.md`
- `docs/database/migrations.md`
- `docs/decisions/ADR-011-postgresql-and-database-naming.md`

## Incluído
- dependências Testcontainers necessárias (`spring-boot-testcontainers`, `testcontainers-postgresql`,
  `testcontainers-junit-jupiter`), com versões gerenciadas pelo BOM do Spring Boot;
- separação entre testes hermáticos (`mvn test` / Surefire) e testes de integração (`mvn verify` / Failsafe,
  convenção `*IT`);
- profile de teste dedicado `it` (`application-it.yml`, em `src/test/resources`), sem exclusão de
  `DataSourceAutoConfiguration`/`FlywayAutoConfiguration`;
- `PostgresIntegrationTestConfiguration` reutilizável (`@TestConfiguration` + `@ServiceConnection`) para futuros
  testes de integração de repositórios;
- teste mínimo de infraestrutura (`FlywayMigrationIT`) validando que a migration `V1` foi aplicada com sucesso.

## Fora do escopo
- repositórios de negócio;
- entidades JPA de negócio;
- tabelas Empresa/Filial/ContaFinanceira;
- Use Cases funcionais;
- autenticação;
- autorização;
- Kafka;
- E2E;
- configuração completa de CI/CD;
- idempotência financeira.

## Restrições
Não:
- usar H2 como substituto do PostgreSQL;
- introduzir tabelas ou entidades de negócio;
- expor testes hermáticos existentes (profile `test`) a Docker;
- alterar `application-test.yml`.

## Critérios de aceite
- [ ] dependências Testcontainers configuradas (versões geridas pelo BOM);
- [ ] `mvn test` continua hermático (sem Docker, profile `test` inalterado);
- [ ] `mvn verify` sobe PostgreSQL 16 via Testcontainers, aplica Flyway real e inicializa o
      `ApplicationContext`;
- [ ] `PostgresIntegrationTestConfiguration` expõe o container via `@ServiceConnection`;
- [ ] `FlywayMigrationIT` confirma que a migration `V1` foi registrada como aplicada com sucesso, sem depender
      da contagem total de linhas em `flyway_schema_history`;
- [ ] nenhuma tabela/entidade/repository de negócio introduzida;
- [ ] nenhuma autenticação/autorização/Kafka introduzida;
- [ ] `mvn package` passa;
- [ ] `mvn verify` validado em ambiente com Docker disponível (pendente de validação em ambientes sem Docker).
