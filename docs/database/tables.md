# Database Tables

Nomes físicos usam camelCase e preservam casing no PostgreSQL.

Principais tabelas:
`"company"`, `"branch"`, `"partner"`, `"category"`, `"costCenter"`, `"bankAccount"`, `"paymentMethod"`, `"financialAccount"`, `"installment"`, `"movimentacaoFinanceira"`, `"historicoConta"`, `"usuario"`, `"usuarioEmpresa"`, `"usuarioEmpresaPerfil"`, `"perfil"`, `"perfilPermissao"`, `"permissao"`.

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
`"totalAmount"` e `"status"`. `"installment"` contém `"id"`,
`"financialAccountId"`, `"installmentNumber"`, `"dueDate"` e `"amount"`.
Valores monetários usam `NUMERIC(19,2)`.
