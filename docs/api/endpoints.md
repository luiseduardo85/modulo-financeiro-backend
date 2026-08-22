# Initial API Endpoints

## Conta
POST /api/v1/contas
GET /api/v1/contas
GET /api/v1/contas/{id}
PUT /api/v1/contas/{id}
DELETE /api/v1/contas/{id}
POST /api/v1/contas/{id}/enviar-aprovacao
POST /api/v1/contas/{id}/aprovar
POST /api/v1/contas/{id}/rejeitar
POST /api/v1/contas/{id}/cancelar
POST /api/v1/contas/{id}/parcelas/{parcelaId}/liquidacoes
POST /api/v1/contas/{id}/movimentacoes/{movimentacaoId}/estornar
GET /api/v1/contas/{id}/historico

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
