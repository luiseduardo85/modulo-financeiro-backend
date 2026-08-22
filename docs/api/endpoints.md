# Initial API Endpoints

## FinancialAccount (FUNC-005 / FUNC-006)
POST /api/v1/companies/{companyId}/financial-accounts
GET /api/v1/companies/{companyId}/financial-accounts
GET /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}
POST /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/submit-for-approval
POST /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/approve
POST /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/reject
POST /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/installments/{installmentId}/settlements
POST /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/installments/{installmentId}/settlements/{movementId}/reversals

Criação recebe `branchId`, `type`, `partnerId`, `categoryId`, `costCenterId`
opcional, `issueDate`, `totalAmount` e `installments`. Retorna 201 e `Location`.
GET por ID retorna o Aggregate completo; listagem retorna somente resumos, sem
`installments`. O `companyId` da rota é escopo do recurso, não contexto
autenticado de tenant. FUNC-005 não exige `Idempotency-Key`.

As ações de FUNC-006 obtêm o ator somente por `ApprovalActorContext`, não exigem
`Idempotency-Key` e retornam o FinancialAccount completo atualizado. Submit e
approve não possuem campos de negócio; reject recebe exatamente `justification`.

Settlement em FUNC-007 exige `Idempotency-Key` e recebe exatamente `amount`,
`movementDate`, `bankAccountId` e `paymentMethodId`. Retorna 201 e representa a
FinancialMovement imutável criada, com `PAYMENT`/`RECEIPT` derivado do tipo da
FinancialAccount. Replay concluído retorna a mesma identidade e representação.

Reversal em FUNC-008 exige `Idempotency-Key` e recebe exatamente `amount`,
`movementDate`, `bankAccountId` e `paymentMethodId`; `movementId` no path
identifica a FinancialMovement original a reverter. Retorna 201 e representa a
FinancialMovement de reversal imutável criada, com `REVERSAL_PAYMENT`/
`REVERSAL_RECEIPT` derivado do tipo da movimentação original e
`originalMovementId` apontando para ela. Replay concluído retorna a mesma
identidade e representação.

Endpoints futuros, fora de FUNC-008:
PUT /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}
DELETE /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}
POST /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/cancel
GET /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/history

Outros recursos:
partners, categories, cost-centers, bank-accounts, payment-methods, companies, branches, usuarios, perfis, configuracoes-aprovacao, fluxo-caixa, dashboard/financeiro, relatorios e me.

## BankAccount / PaymentMethod

```text
POST /api/v1/companies/{companyId}/bank-accounts
GET  /api/v1/companies/{companyId}/bank-accounts
GET  /api/v1/companies/{companyId}/bank-accounts/{bankAccountId}
POST /api/v1/companies/{companyId}/bank-accounts/{bankAccountId}/deactivate
POST /api/v1/companies/{companyId}/payment-methods
GET  /api/v1/companies/{companyId}/payment-methods
GET  /api/v1/companies/{companyId}/payment-methods/{paymentMethodId}
POST /api/v1/companies/{companyId}/payment-methods/{paymentMethodId}/deactivate
```

BankAccount recebe `name` e `branchId` opcional; PaymentMethod recebe somente
`name`. O `companyId` da rota e escopo do recurso, nao contexto autenticado de
tenant. Criacao retorna 201 e `Location`; demais operacoes retornam 200.

## Category / CostCenter

```text
POST /api/v1/companies/{companyId}/categories
GET  /api/v1/companies/{companyId}/categories
GET  /api/v1/companies/{companyId}/categories/{categoryId}
POST /api/v1/companies/{companyId}/categories/{categoryId}/deactivate
POST /api/v1/companies/{companyId}/cost-centers
GET  /api/v1/companies/{companyId}/cost-centers
GET  /api/v1/companies/{companyId}/cost-centers/{costCenterId}
POST /api/v1/companies/{companyId}/cost-centers/{costCenterId}/deactivate
```

Criacoes recebem somente `name`. O `companyId` da rota e apenas escopo do
recurso e ainda nao representa contexto de tenant autenticado.

## Partner

POST /api/v1/partners
GET /api/v1/partners/{id}
GET /api/v1/partners
POST /api/v1/partners/{id}/deactivate

## Company / Branch

Os endpoints canônicos deste slice usam inglês:

```text
POST /api/v1/companies
GET  /api/v1/companies/{id}
GET  /api/v1/companies
POST /api/v1/companies/{companyId}/branches
GET  /api/v1/companies/{companyId}/branches/{branchId}
GET  /api/v1/companies/{companyId}/branches
```

Criações recebem somente `name`. Company responde com `id` e `name`; Branch
responde com `id`, `companyId` e `name`.
