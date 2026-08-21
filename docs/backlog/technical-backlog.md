# Technical Backlog v1.0

# TECH-001 — Bootstrap Spring Boot

## Objetivo

Criar o projeto backend inicial utilizando Java 21,
Spring Boot e Maven.

A tarefa deve estabelecer apenas a fundação necessária
para as próximas etapas técnicas.

## Documentação relacionada

- .github/copilot-instructions.md
- docs/architecture/overview.md
- docs/architecture/backend-architecture.md
- docs/backlog/technical-backlog.md

## Escopo incluído

- Java 21
- Spring Boot
- Maven
- aplicação inicial
- application.yml
- profile local
- profile test
- teste básico de inicialização

## Fora do escopo

- PostgreSQL
- Flyway
- JPA
- Testcontainers
- domínio financeiro
- autenticação
- autorização
- Kafka

## Critérios de aceite

- [ ] projeto compila
- [ ] aplicação inicializa
- [ ] mvn test executa com sucesso
- [ ] mvn package executa com sucesso
- [ ] teste de contexto existe
- [ ] nenhuma feature de negócio foi implementada
- [ ] nenhuma integração de autenticação foi criada
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

## TECH-003 — PostgreSQL local

### Objetivo
Configurar ambiente local de PostgreSQL.

### Inclui
- Docker Compose
- datasource Spring
- variáveis de ambiente
- validação de conexão

### Regra
Banco usa camelCase.

### Prioridade
P0

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
