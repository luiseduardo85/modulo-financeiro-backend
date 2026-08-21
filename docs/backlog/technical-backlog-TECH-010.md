# Technical Backlog — TECH-010

## TECH-010 — Infraestrutura de idempotência financeira

### Objetivo
Definir e implementar a base técnica de idempotência para futuras operações financeiras críticas, impedindo processamento duplicado em retries, timeouts ou reenvios.

Esta TECH não implementa liquidação, pagamento, recebimento ou estorno.

### Dependências
- TECH-003 PostgreSQL
- TECH-004 Flyway
- TECH-005 Testcontainers
- TECH-006 API errors
- TECH-007 persistence
- TECH-008 testing
- TECH-009 observability

### Escopo
- contrato `Idempotency-Key`;
- escopo de unicidade multiempresa;
- fingerprint do comando;
- persistência técnica PostgreSQL;
- constraint de unicidade;
- concorrência;
- estratégia transacional;
- conflito técnico HTTP 409;
- testes PostgreSQL 16 Testcontainers;
- documentação para consumo por futuros Use Cases.

### Regras iniciais
A chave é fornecida pelo cliente e tratada como identificador opaco.

A unicidade deve considerar pelo menos:
`companyId + operation + idempotencyKey`.

A mesma chave com comando materialmente diferente deve gerar conflito.

A correção de concorrência deve depender de PostgreSQL/constraint/transação, nunca de memória local.

### Persistência
Uma tabela técnica de idempotência pode ser criada.

Usar:
- Flyway;
- PostgreSQL 16;
- camelCase;
- quoted identifiers;
- JPA apenas na Infrastructure.

Não criar tabela financeira.

### Fora do escopo
- ContaFinanceira;
- Parcela;
- MovimentacaoFinanceira;
- liquidação;
- pagamento;
- recebimento;
- estorno;
- autenticação/autorização;
- Kafka;
- Redis;
- outbox;
- retry engine;
- TTL/cleanup/scheduler.

### Critérios de aceite
- [ ] Idempotency-Key documentado
- [ ] escopo de unicidade definido
- [ ] fingerprint definido
- [ ] unicidade garantida pelo PostgreSQL
- [ ] reutilização incompatível gera conflito
- [ ] concorrência não depende de memória
- [ ] transação futura documentada
- [ ] integration tests com PostgreSQL 16
- [ ] nenhuma operação financeira real implementada
