# Database Tables

Nomes físicos usam camelCase e preservam casing no PostgreSQL.

Principais tabelas:
`"company"`, `"branch"`, `"partner"`, `"category"`, `"costCenter"`, `"bankAccount"`, `"paymentMethod"`, `"financialAccount"`, `"installment"`, `"financialMovement"`, `"historicoConta"`, `"usuario"`, `"usuarioEmpresa"`, `"usuarioEmpresaPerfil"`, `"perfil"`, `"perfilPermissao"`, `"permissao"`.

`"bankAccount"` contem somente `"id"`, `"companyId"`, `"branchId"`, `"name"`
e `"active"`. `"branchId"` e anulavel. FKs garantem que Company e Branch
existam; a aplicacao valida que a Branch pertence a mesma Company, pois o modelo
existente de Branch nao possui chave composta. `"paymentMethod"` contem somente
`"id"`, `"companyId"`, `"name"` e `"active"`.

`"partner"` contém `"id"`, `"name"`, `"document"`, `"customer"`,
`"supplier"` e `"active"`. Documento é canônico, globalmente único e não há
coluna `"companyId"` ou `"documentType"`. CPF usa 11 dígitos; CNPJ usa 12
posições alfanuméricas maiúsculas seguidas de dois dígitos verificadores.

`"company"` contém somente `"id"` e `"name"`. `"branch"` contém somente
`"id"`, `"companyId"` e `"name"`, com FK obrigatória de `"branch"."companyId"`
para `"company"."id"`. Nomes têm no máximo 200 caracteres, não podem ser
brancos após `BTRIM` e não possuem constraints de unicidade.

`"financialAccount"` contém `"id"`, `"companyId"`, `"branchId"`, `"type"`,
`"partnerId"`, `"categoryId"`, `"costCenterId"`, `"issueDate"`,
`"totalAmount"`, `"status"` e `"version"` (controle otimista exclusivamente
de persistência). `"installment"` contém `"id"`,
`"financialAccountId"`, `"installmentNumber"`, `"dueDate"` e `"amount"`.
Valores monetários usam `NUMERIC(19,2)`.

FUNC-006 adiciona `"approvalConfiguration"`, `"approvalRequest"` e
`"approvalDecision"`. Elas usam IDs escalares e preservam configuração,
solicitante, decisor e justificativa sem criar tabela genérica de histórico ou
FK para ator externo.

FUNC-007 adiciona `"financialMovement"` com `"id"`, `"installmentId"`,
`"type"`, `"amount"`, `"movementDate"`, `"bankAccountId"` e
`"paymentMethodId"`. Não há companyId/financialAccountId redundantes, saldo,
ator, status, timestamps ou campos de Reversal.
