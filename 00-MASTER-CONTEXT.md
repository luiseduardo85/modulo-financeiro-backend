# MASTER CONTEXT — SaaS Financeiro

Leia AGENTS.md antes de qualquer tarefa.

## Stack
Java 21, Spring Boot 4.1.1, Maven, PostgreSQL 16, Flyway, JPA/Hibernate, Testcontainers.
Frontend: React + TypeScript + Tailwind.

## Arquitetura
Clean Architecture + DDD pragmático.
Package por contexto: `com.financeiro.<context>/{domain,application,infrastructure,interfaces}`.
Domain sem Spring/JPA/Jackson/HTTP/PostgreSQL.
Application: use cases, ports, transações.
Infrastructure: JPA/PostgreSQL/Flyway.
Interfaces: REST DTO/controller/error mapping.
Domain Entity e JPA Entity separados.
Não criar BaseEntity/BaseRepository/BaseService/BaseController/GenericCrud.
Flyway é dono do schema. `ddl-auto=validate`. Sem H2.
Money: BigDecimal/NUMERIC(19,2), sem float/double e sem rounding silencioso.
Enums persistidos como string. IDs Long/BIGINT identity.
Multiempresa sempre preservado.

## Precedência
1. ADRs
2. Business Rules
3. Domain Model
4. Use Cases
5. API Contracts
6. Código

## Qualidade
Sempre executar:
`.\mvnw.cmd spotless:apply`
`.\mvnw.cmd spotless:check`
`.\mvnw.cmd test`
`.\mvnw.cmd package`
`.\mvnw.cmd verify`
`git diff --check`

## Git
Não push.
Não reset --hard, restore ., stash, rebase, amend.
Preservar planning artifacts.
Não usar `git add .`/`git add -A` com mudanças não relacionadas.
Uma migration nova por slice quando necessário; migrations anteriores imutáveis.

## Já concluído
TECH-001..TECH-010
FUNC-001 Company/Branch
FUNC-002 Partner
FUNC-003 Category/CostCenter
FUNC-004 BankAccount/PaymentMethod
FUNC-005 FinancialAccount/Installments
FUNC-006 Approval Workflow

FinancialAccount types: PAYABLE, RECEIVABLE.
Statuses: DRAFT, PENDING_APPROVAL, APPROVED, SETTLED, CANCELLED.
Não persistir PARTIALLY_SETTLED, OVERDUE, REVERSED.

## TECH-010
Idempotency-Key 1..128 ASCII visível.
Scope companyId + operation + key.
SHA-256 fingerprint.
PROCESSING/COMPLETED.
Claim + efeito financeiro na mesma transação.
Mesma key/fingerprint diferente => 409.
resultReference aponta para efeito persistido.

## FUNC-007 aprovado
FinancialMovement:
id, installmentId, type, amount, movementDate, bankAccountId, paymentMethodId.
Types PAYMENT/RECEIPT.
PAYABLE->PAYMENT, RECEIVABLE->RECEIPT.
Somente APPROVED é liquidável.
Parcial permitido. Overpayment proibido.
Balance derivado por SUM.
SETTLED somente quando todas Installments zerarem.
BankAccount e PaymentMethod obrigatórios/ativos/same-company; branch da conta bancária null global ou igual à FinancialAccount.
movementDate LocalDate obrigatório.
TECH-010 obrigatório, operação SETTLE_INSTALLMENT.
Sem actor/userId. CONTA_LIQUIDAR permanece conceitual com enforcement adiado.
Reversal é FUNC-008.

## Estado atual
FUNC-007 está implementado, mas o último review pediu correções antes do commit:
- FinancialMovement record expõe construtor com ID.
- faltam provas de rollback após insert do movimento/falha final.
- faltam alguns testes edge/isolation/MVC/PostgreSQL.
- pequenos ajustes de docs.
