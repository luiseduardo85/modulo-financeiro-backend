# Database Indexes

Índices guiados por consultas reais.

Candidatos:
- FinancialAccount por `companyId` usa `"ixFinancialAccountCompanyId"`;
- empresa+filial;
- Partner usa somente o índice criado pela constraint UNIQUE de documento neste slice;
- Category por `companyId` usa `"ixCategoryCompanyId"`;
- CostCenter por `companyId` usa `"ixCostCenterCompanyId"`;
- BankAccount por `companyId` usa `"ixBankAccountCompanyId"`; nao ha indice de
  `branchId` porque o slice nao consulta por esse campo;
- PaymentMethod por `companyId` usa `"ixPaymentMethodCompanyId"`;
- Installment usa a UNIQUE iniciada por `financialAccountId`; FUNC-005 não cria
  índice separado nem consulta por vencimento;
- movimentação por parcela+data;
- movimentação original;
- joins de autorização.

Nomes também usam camelCase.
