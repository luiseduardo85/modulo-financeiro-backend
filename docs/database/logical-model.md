# Logical Database Model

Company -> Branch
Company -> Category
Company -> CostCenter
Company -> BankAccount
Company -> PaymentMethod
BankAccount -> Branch (opcional; restricao a uma unica Branch)
Empresa -> ContaFinanceira -> ContaFinanceiraParcela -> MovimentacaoFinanceira

ContaFinanceira referencia Partner, Category e CostCenter.
Partner é global e não possui relação com Company.

Usuario -> UsuarioEmpresa -> UsuarioEmpresaPerfil -> Perfil -> PerfilPermissao -> Permissao.

HistoricoConta referencia ContaFinanceira e é persistido separadamente do carregamento normal do Aggregate.
