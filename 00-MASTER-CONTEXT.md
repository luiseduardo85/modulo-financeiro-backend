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
FUNC-007 Settlement (branch `func/func-007-settlement`)
FUNC-008 Reversal (branch `func/func-008-reversal`)
FUNC-009 Financial Account History (branch `func/func-009-financial-history`)

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

## FUNC-008 aprovado
Reversal append-only de FinancialMovement, sem entidade separada:
`FinancialMovementType` ganha REVERSAL_PAYMENT/REVERSAL_RECEIPT.
`originalMovementId` adicionado à FinancialMovement (nulo em PAYMENT/RECEIPT,
obrigatório e positivo em reversal). Nunca reverter um reversal.
Reversal parcial permitido; soma dos reversals <= valor original.
Balance derivado agora é líquido (pagamentos/recebimentos - reversals).
SETTLED -> APPROVED quando o reversal reabre saldo.
Mesma serialização otimista da FinancialAccount usada pelo settlement
(FUNC-007) é reutilizada para impedir over-reversal concorrente.
TECH-010 obrigatório, operação REVERSE_FINANCIAL_MOVEMENT.
Sem actor/userId. Movimento efetivo nunca é apagado ou atualizado (sem
UPDATE/DELETE em FinancialMovement).
V11: coluna `originalMovementId`, FK auto-referenciada sem cascade, `type`
ampliado para VARCHAR(20), CHECK de consistência tipo/originalMovementId.

## FUNC-009 aprovado
Histórico/audit trail de FinancialAccount sem event sourcing.
Endpoint compõe uma timeline a partir de 4 fontes, sem duplicar evidência já
existente: `FinancialAccountHistory` (somente CREATED e
APPROVED_WITHOUT_WORKFLOW, os únicos eventos sem evidência própria),
`ApprovalRequest`/`ApprovalDecision` (submissão/decisão de workflow) e
`FinancialMovement` (settlement/reversal).
`actorId` somente quando existe trusted actor real (CREATED nunca tem ator;
APPROVED_WITHOUT_WORKFLOW usa o ator do ApprovalActorContext do submit).
Histórico append-only, sem update/delete.
Ordenação determinística via `createdAt` técnico UTC (DEFAULT now(), não
mapeado nas Entities JPA) adicionado a approvalRequest/approvalDecision/
financialMovement, com desempate por fase (criação, aprovação, movimento) e
ID de origem.
V12: tabela `financialAccountHistory` + as 3 colunas `createdAt`.
Company scope validado carregando a FinancialAccount antes de montar a
timeline.

## Estado atual
FUNC-007, FUNC-008 e FUNC-009 implementados, revisados e commitados.
`func/func-008-reversal` foi pushado para `origin` a pedido explícito do
usuário (contém FUNC-007 + FUNC-008); `func/func-009-financial-history`
(branch atual, criada sobre func-008) ainda não foi pushada. PR para `main`
ainda não foi aberto (nenhuma slice de FUNC-002 em diante está mergeada em
`main`).
Próximo passo: FUNC-010 (Cash Flow).
