# Technical Backlog — TECH-004

## TECH-004 — Configurar Flyway

### Objetivo
Configurar o Flyway como mecanismo oficial de evolução do schema PostgreSQL do backend.

A tarefa deve estabelecer a infraestrutura de migrations e validar que o backend consegue aplicar migrations versionadas no PostgreSQL local.

Nenhuma tabela de negócio deve ser criada nesta tarefa.

### Prioridade
P0

### Dependências
- TECH-001 — Bootstrap Spring Boot
- TECH-002 — Estrutura Clean Architecture
- TECH-003 — PostgreSQL local

### Documentação relacionada
- `.github/copilot-instructions.md`
- `docs/architecture/persistence.md`
- `docs/database/migrations.md`
- `docs/decisions/ADR-011-postgresql-and-database-naming.md`
- `docs/backlog/technical-backlog.md`

### Escopo incluído
- adicionar dependência Flyway;
- adicionar suporte específico do Flyway para PostgreSQL, quando exigido pela versão adotada;
- configurar Spring Boot + Flyway;
- criar `backend/src/main/resources/db/migration/`;
- definir convenção de migrations;
- validar execução contra PostgreSQL local;
- validar que migrations já aplicadas não sejam reaplicadas;
- documentar o uso mínimo do Flyway.

### Convenção
Formato:

`V<versao>__<descricao>.sql`

Exemplos:
- `V1__initialize_database.sql`
- `V2__create_company_tables.sql`

Migration aplicada nunca deve ser editada.

### PostgreSQL
- PostgreSQL 16.
- Identificadores usam camelCase.
- Usar quoted identifiers quando necessário para preservar camelCase.
- Não introduzir snake_case.

### Schema ownership
Flyway é o único responsável pela evolução do schema.

Não utilizar:
- `ddl-auto=update`
- `create`
- `create-drop`

JPA/Hibernate não devem ser adicionados nesta tarefa.

### Migration inicial
Preferir não antecipar tabelas de negócio.

Se uma `V1` técnica for necessária para validar a infraestrutura, ela deve ser mínima e não criar entidades do domínio.

### Validação
1. subir PostgreSQL local;
2. iniciar backend com profile local;
3. confirmar inicialização do Flyway;
4. confirmar criação da tabela de histórico do Flyway;
5. confirmar aplicação das migrations pendentes;
6. reiniciar a aplicação;
7. confirmar que migrations já aplicadas não são executadas novamente;
8. executar build/testes.

### Fora do escopo
- tabelas de negócio;
- JPA/Hibernate;
- repositories;
- Testcontainers;
- autenticação;
- autorização;
- Kafka;
- seeds de negócio;
- UCs funcionais.

### Critérios de aceite
- [ ] Flyway configurado
- [ ] suporte PostgreSQL configurado
- [ ] pasta `db/migration` criada
- [ ] convenção documentada
- [ ] aplicação local executa Flyway
- [ ] histórico do Flyway é criado
- [ ] reinício não reaplica migrations
- [ ] nenhuma tabela de negócio criada
- [ ] JPA/Hibernate não introduzido
- [ ] Testcontainers não introduzido
- [ ] autenticação não implementada
- [ ] `mvn test` passa
- [ ] `mvn package` passa
