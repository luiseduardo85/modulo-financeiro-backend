# Logical Database Model

Company -> Branch
Company -> Category
Company -> CostCenter
Company -> BankAccount
Company -> PaymentMethod
BankAccount -> Branch (opcional; restricao a uma unica Branch)
Company -> FinancialAccount -> Installment
Installment -> FinancialMovement

FinancialAccount referencia Branch, Partner, Category e CostCenter opcional por
IDs escalares.
FinancialMovement é separada do Aggregate carregado de FinancialAccount e
referencia Installment por ID escalar. Company e FinancialAccount são derivados
pela cadeia de ownership, sem colunas redundantes.
Partner é global e não possui relação com Company.

Usuario -> UsuarioEmpresa -> UsuarioEmpresaPerfil -> Perfil -> PerfilPermissao -> Permissao.

HistoricoConta referencia ContaFinanceira e é persistido separadamente do carregamento normal do Aggregate.
