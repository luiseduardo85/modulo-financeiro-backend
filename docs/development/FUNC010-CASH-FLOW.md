# FUNC-010 — Cash Flow

Fluxo autônomo completo. Branch `func/func-010-cash-flow`. Não push.

Objetivo: API de projeção de fluxo de caixa. É leitura/projeção, não nova fonte de verdade.

Planeje usando docs/telas.
Esperado:
- previsto: Installment.dueDate + remaining balance;
- realizado: FinancialMovement.movementDate + efeito líquido;
- PAYABLE reduz caixa;
- RECEIVABLE aumenta;
- reversal desfaz sinal;
- company scoped;
- branch filtering quando aplicável;
- partial settlement considerado;
- OVERDUE derivado, não persistido;
- não criar ledger bancário;
- não persistir totalizações;
- SQL/projections, sem N+1.

API provável:
GET `/api/v1/companies/{companyId}/cash-flow`

Adicione migration de índice somente se query real justificar (V13 se necessária).
Testes de sinais, partial, reversal, datas, company/branch isolation, empty state, SQL aggregate.
Review, validate, commit:
`feat: implement cash flow projection`
