# Database Indexes

Índices guiados por consultas reais.

Candidatos:
- conta por empresa+status;
- empresa+filial;
- parceiro;
- categoria;
- BankAccount por `companyId` usa `"ixBankAccountCompanyId"`; nao ha indice de
  `branchId` porque o slice nao consulta por esse campo;
- PaymentMethod por `companyId` usa `"ixPaymentMethodCompanyId"`;
- parcela por vencimento;
- movimentação por parcela+data;
- movimentação original;
- joins de autorização.

Nomes também usam camelCase.
