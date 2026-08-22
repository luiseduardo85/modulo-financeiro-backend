# Logical Database Model

Company -> Branch
Company -> Category
Company -> CostCenter
Company -> BankAccount
Company -> PaymentMethod
BankAccount -> Branch (opcional; restricao a uma unica Branch)
Company -> FinancialAccount -> Installment

FinancialAccount referencia Branch, Partner, Category e CostCenter opcional por
IDs escalares. Movimentação financeira permanece fora do FUNC-005.
Partner é global e não possui relação com Company.

Usuario -> UsuarioEmpresa -> UsuarioEmpresaPerfil -> Perfil -> PerfilPermissao -> Permissao.

HistoricoConta referencia ContaFinanceira e é persistido separadamente do carregamento normal do Aggregate.
