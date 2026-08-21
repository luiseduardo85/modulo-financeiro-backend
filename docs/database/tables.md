# Database Tables

Nomes físicos usam camelCase e preservam casing no PostgreSQL.

Principais tabelas:
`"empresa"`, `"filial"`, `"parceiro"`, `"categoria"`, `"centroCusto"`, `"contaBancaria"`, `"formaFinanceira"`, `"contaFinanceira"`, `"contaFinanceiraParcela"`, `"movimentacaoFinanceira"`, `"historicoConta"`, `"usuario"`, `"usuarioEmpresa"`, `"usuarioEmpresaPerfil"`, `"perfil"`, `"perfilPermissao"`, `"permissao"`.

`"contaFinanceira"` contém, entre outros:
`"id"`, `"empresaId"`, `"filialId"`, `"parceiroId"`, `"categoriaId"`, `"centroCustoId"`, `"tipo"`, `"status"`, `"valorTotal"`, `"dataEmissao"`, `"createdAt"`, `"updatedAt"`, `"version"`.

`"contaFinanceiraParcela"` contém `"contaFinanceiraId"`, `"numero"`, `"valor"`, `"dataVencimento"`.
Saldo não é fonte de verdade persistida.
