# Logical Database Model

Company -> Branch
Company -> BankAccount
Company -> PaymentMethod
BankAccount -> Branch (opcional; restricao a uma unica Branch)
Empresa -> ContaFinanceira -> ContaFinanceiraParcela -> MovimentacaoFinanceira

ContaFinanceira referencia Parceiro, Categoria e CentroCusto.

Usuario -> UsuarioEmpresa -> UsuarioEmpresaPerfil -> Perfil -> PerfilPermissao -> Permissao.

HistoricoConta referencia ContaFinanceira e é persistido separadamente do carregamento normal do Aggregate.
