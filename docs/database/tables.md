# Database Tables

Nomes físicos usam camelCase e preservam casing no PostgreSQL.

Principais tabelas:
`"company"`, `"branch"`, `"parceiro"`, `"categoria"`, `"centroCusto"`, `"contaBancaria"`, `"formaFinanceira"`, `"contaFinanceira"`, `"contaFinanceiraParcela"`, `"movimentacaoFinanceira"`, `"historicoConta"`, `"usuario"`, `"usuarioEmpresa"`, `"usuarioEmpresaPerfil"`, `"perfil"`, `"perfilPermissao"`, `"permissao"`.

`"company"` contém somente `"id"` e `"name"`. `"branch"` contém somente
`"id"`, `"companyId"` e `"name"`, com FK obrigatória de `"branch"."companyId"`
para `"company"."id"`. Nomes têm no máximo 200 caracteres, não podem ser
brancos após `BTRIM` e não possuem constraints de unicidade.

`"contaFinanceira"` contém, entre outros:
`"id"`, `"empresaId"`, `"filialId"`, `"parceiroId"`, `"categoriaId"`, `"centroCustoId"`, `"tipo"`, `"status"`, `"valorTotal"`, `"dataEmissao"`, `"createdAt"`, `"updatedAt"`, `"version"`.

`"contaFinanceiraParcela"` contém `"contaFinanceiraId"`, `"numero"`, `"valor"`, `"dataVencimento"`.
Saldo não é fonte de verdade persistida.
