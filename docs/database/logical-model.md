# Logical Database Model

Company -> Branch
Company -> Category
Company -> CostCenter
Company -> BankAccount
Company -> PaymentMethod
BankAccount -> Branch (opcional; restricao a uma unica Branch)
Company -> FinancialAccount -> Installment
Installment -> FinancialMovement
FinancialMovement -> FinancialMovement (originalMovementId, opcional, auto-referenciado)
FinancialAccount -> FinancialAccountHistory

FinancialAccount referencia Branch, Partner, Category e CostCenter opcional por
IDs escalares.
FinancialMovement é separada do Aggregate carregado de FinancialAccount e
referencia Installment por ID escalar. Company e FinancialAccount são derivados
pela cadeia de ownership, sem colunas redundantes. Um reversal (FUNC-008)
referencia a FinancialMovement original por `originalMovementId`; a coluna é
nula para `PAYMENT`/`RECEIPT` e obrigatória para `REVERSAL_PAYMENT`/
`REVERSAL_RECEIPT`. Um reversal nunca referencia outro reversal.
Partner é global e não possui relação com Company.

Usuario -> UsuarioEmpresa -> UsuarioEmpresaPerfil -> Perfil -> PerfilPermissao -> Permissao.

FinancialAccountHistory referencia FinancialAccount e é persistido
separadamente do carregamento normal do Aggregate. A timeline de histórico
(FUNC-009) não lê somente essa tabela: compõe eventos de
FinancialAccountHistory, ApprovalRequest/ApprovalDecision e FinancialMovement,
todos derivados transitivamente de FinancialAccount.
