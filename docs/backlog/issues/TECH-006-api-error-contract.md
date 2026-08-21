# TECH-006 — Implementar contrato de erros da API

## Objetivo

Criar a infraestrutura comum de tratamento de erros REST, padronizando status HTTP e payloads de erro.

## Dependências

- TECH-001
- TECH-002
- TECH-003
- TECH-004
- TECH-005

## Documentação relacionada

- `AGENTS.md`
- `docs/api/errors.md`
- `docs/api/conventions.md`
- `docs/architecture/backend-architecture.md`

## Escopo incluído

- ErrorResponse
- ValidationErrorDetail
- GlobalExceptionHandler
- Bean Validation
- request/JSON malformado
- mapeamento técnico de 404/409/422/500
- timestamp
- traceId mínimo
- testes de contrato

## Fora do escopo

- regras financeiras;
- erros de ContaFinanceira;
- autenticação;
- autorização completa;
- observabilidade avançada;
- Kafka;
- i18n completa.

## Critérios de aceite

- [ ] contrato de erro estável
- [ ] validação de campos padronizada
- [ ] JSON malformado tratado
- [ ] recurso não encontrado suportado
- [ ] conflitos/validação semântica possuem base de mapeamento
- [ ] erro interno não vaza stack trace
- [ ] timestamp ISO-8601
- [ ] traceId tratado de forma mínima
- [ ] testes passam
- [ ] Domain permanece sem dependência HTTP
- [ ] nenhuma autenticação implementada
