# Database Constraints

Usar PK, FK, UNIQUE e NOT NULL para integridade estrutural.

Exemplos:
- `partner.document` único globalmente e ao menos um de `customer`/`supplier` verdadeiro;
- `financialAccountId` + `installmentNumber` únicos;
- permissao.codigo único;
- associações usuário+empresa e usuário+empresa+perfil sem duplicidade;
- perfil+permissão sem duplicidade.
- BankAccount possui FKs independentes para Company e Branch opcional; a
  igualdade entre a Company da conta e a Company da Branch e validada na
  aplicacao para nao distorcer a PK simples existente de Branch;
- PaymentMethod possui FK obrigatoria para Company;
- nomes de BankAccount e PaymentMethod nao sao unicos.

Não usar ON DELETE CASCADE indiscriminadamente em dados financeiros.

FinancialAccount possui FKs escalares para Company, Branch, Partner, Category e
CostCenter opcional. A aplicação valida o escopo de Company para Branch,
Category e CostCenter; as FKs simples não afirmam essa igualdade entre colunas.

ApprovalConfiguration possui FKs escalares independentes para Company e Branch
opcional; a Application valida que a Branch pertence à mesma Company.
ApprovalRequest referencia FinancialAccount sem cascade e admite no máximo um
registro PENDING por conta. ApprovalDecision referencia ApprovalRequest sem
cascade, admite uma decisão por request e exige justificativa somente para
REJECTED.

FinancialMovement possui FKs escalares para Installment, BankAccount e
PaymentMethod, sem cascade; CHECK restringe tipo a `PAYMENT`/`RECEIPT` e amount
positivo. PostgreSQL não prova tipo financeiro, saldo restante, Company/Branch
das referências ou estado ativo; Application aplica essas regras sob a
serialização otimista da FinancialAccount.
