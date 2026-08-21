# Technical Backlog v1.0

## TECH-001 — Bootstrap Spring Boot

### Objetivo
Criar o projeto backend inicial.

### Inclui
- Java
- Spring Boot
- Maven
- configuração inicial
- profiles básicos

### Não inclui
- domínio financeiro
- autenticação
- banco funcional completo

### Prioridade
P0

---

## TECH-002 — Estrutura Clean Architecture

### Objetivo
Criar a organização inicial por contexto e camada.

### Esperado
```text
context/
  domain/
  application/
  infrastructure/
  interfaces/
```

### Prioridade
P0

---

## TECH-003 — Configurar PostgreSQL local

### Objetivo

Configurar o PostgreSQL como banco de dados local do backend, permitindo que a aplicação estabeleça conexão com uma instância PostgreSQL executada via Docker Compose.

Esta tarefa deve preparar somente a infraestrutura de conexão local com o banco. Não deve criar o modelo de dados do domínio financeiro.

### Prioridade

P0

### Dependências

- TECH-001 — Bootstrap Spring Boot
- TECH-002 — Estrutura Clean Architecture

### Documentação relacionada

- `.github/copilot-instructions.md`
- `docs/architecture/persistence.md`
- `docs/database/migrations.md`
- `docs/decisions/ADR-011-postgresql-and-database-naming.md`

### Escopo incluído

- adicionar PostgreSQL JDBC Driver;
- adicionar somente dependências mínimas necessárias à conexão;
- criar ou ajustar `docker-compose.yml` para PostgreSQL local;
- configurar datasource do profile `local`;
- utilizar variáveis de ambiente para host, porta, database, usuário e senha;
- garantir que credenciais reais não sejam versionadas;
- documentar a execução local mínima;
- validar que a aplicação consegue estabelecer conexão com PostgreSQL.

### Convenções do banco

Banco oficial: PostgreSQL.

Identificadores físicos usam camelCase, por exemplo:

- `"contaFinanceira"`
- `"empresaId"`
- `"dataVencimento"`

Como PostgreSQL converte identificadores não delimitados para lowercase, migrations e SQL nativo devem utilizar quoted identifiers quando necessário para preservar camelCase.

TECH-003 não cria tabelas de negócio.

### Configuração esperada

O profile `local` deve obter configuração de conexão por variáveis de ambiente, por exemplo:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

Os nomes exatos podem ser ajustados durante o Plan, desde que permaneçam consistentes e documentados.

### Docker Compose

O serviço PostgreSQL deve:

- utilizar imagem oficial do PostgreSQL;
- expor a porta necessária ao desenvolvimento local;
- utilizar variáveis de ambiente;
- possuir volume persistente para dados locais;
- não armazenar credenciais de produção;
- poder subir independentemente do backend.

### Validação esperada

A tarefa deve provar que:

1. PostgreSQL sobe via Docker Compose;
2. aplicação inicia usando o profile `local`;
3. backend estabelece conexão com PostgreSQL;
4. projeto continua compilando;
5. testes existentes continuam passando.

### Fora do escopo

- Flyway;
- migrations;
- criação de tabelas;
- entidades JPA do domínio;
- repositories;
- Testcontainers;
- modelagem financeira;
- autenticação;
- autorização;
- Kafka;
- seed de dados de negócio.

### Restrições

Não:

- usar H2;
- usar banco em memória como substituto do PostgreSQL;
- usar `ddl-auto=update`;
- permitir Hibernate criar schema de negócio;
- adicionar tabelas financeiras;
- introduzir snake_case;
- versionar credenciais reais;
- adicionar autenticação.

### Critérios de aceite

- [ ] PostgreSQL JDBC Driver configurado;
- [ ] PostgreSQL pode ser iniciado via Docker Compose;
- [ ] datasource do profile local aponta para PostgreSQL;
- [ ] configuração sensível utiliza variáveis de ambiente;
- [ ] nenhuma credencial real foi versionada;
- [ ] aplicação inicia com PostgreSQL local disponível;
- [ ] conexão com PostgreSQL é validada;
- [ ] nenhuma tabela de domínio foi criada;
- [ ] Flyway não foi implementado nesta tarefa;
- [ ] Testcontainers não foi implementado nesta tarefa;
- [ ] nenhuma autenticação foi implementada;
- [ ] `mvn test` passa;
- [ ] `mvn package` passa.
---

## TECH-004 — Flyway

### Objetivo
Configurar evolução versionada do schema.

### Regras
- migrations em `backend/src/main/resources/db/migration`
- não usar `ddl-auto=update`
- Hibernate valida, Flyway evolui schema

### Prioridade
P0

---

## TECH-005 — PostgreSQL Testcontainers

### Objetivo
Criar base de testes reais de integração com PostgreSQL.

### Critério de aceite
Teste sobe PostgreSQL, executa Flyway e inicializa o ApplicationContext.

### Regra
Não usar H2 como substituto.

### Prioridade
P0

---

## TECH-006 — Contrato de Erros da API

### Objetivo
Implementar infraestrutura comum de erros.

### Componentes esperados
- ErrorResponse
- ValidationErrorDetail
- GlobalExceptionHandler

### Campos
- code
- message
- details
- timestamp
- traceId

### Prioridade
P0

---

## TECH-007 — Convenções de Persistência

### Objetivo
Definir padrões compartilhados.

### Inclui
- IDs
- createdAt
- updatedAt
- version
- estratégia de mapeamento camelCase PostgreSQL

### Restrição
Evitar herança técnica excessiva apenas para reduzir código.

### Prioridade
P0

---

## TECH-008 — Convenções de Testes

### Objetivo
Padronizar testes.

### Tipos
- Domain Unit Test
- Application Test
- Repository Integration Test
- API Test
- E2E

### Prioridade
P0

---

## TECH-009 — Observabilidade Básica

### Objetivo
Preparar observabilidade mínima.

### Inclui
- Actuator
- health
- logging
- traceId

### Não inclui
Stack avançada de observabilidade.

### Prioridade
P0

---

## TECH-010 — Fundação de Idempotência Financeira

### Objetivo
Evitar duplicidade em operações financeiras críticas.

### Aplicação inicial
- liquidação
- estorno

### Conceito
Suporte a `Idempotency-Key` e armazenamento/validação da execução.

### Prioridade
P1

### Dependências
- TECH-003
- TECH-004
- TECH-005
- núcleo financeiro implementado
