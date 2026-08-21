# TECH-010 — Infraestrutura de idempotência financeira

## Objetivo
Preparar a garantia técnica contra duplicidade para operações financeiras futuras.

## Incluído
- Idempotency-Key
- escopo por empresa/operação
- fingerprint
- persistência PostgreSQL
- unique constraint
- concorrência
- transação
- HTTP 409 para conflito
- Testcontainers

## Fora do escopo
- liquidação
- pagamento/recebimento
- estorno
- entidades financeiras
- autenticação/autorização
- Kafka
- Redis
- cleanup/TTL
