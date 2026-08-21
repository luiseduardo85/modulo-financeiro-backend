# Logical Database Model

Empresa -> Filial
Empresa -> ContaBancaria
Empresa -> ContaFinanceira -> ContaFinanceiraParcela -> MovimentacaoFinanceira

ContaFinanceira referencia Parceiro, Categoria e CentroCusto.

Usuario -> UsuarioEmpresa -> UsuarioEmpresaPerfil -> Perfil -> PerfilPermissao -> Permissao.

HistoricoConta referencia ContaFinanceira e é persistido separadamente do carregamento normal do Aggregate.
