# FUNC-005 — Financial Account

## Objetivo

Implementar o núcleo inicial de FinancialAccount para contas a pagar e receber,
conectando os cadastros já concluídos sem ainda implementar liquidação,
movimentação financeira, estorno ou aprovação completa.

## Regras confirmadas

- FinancialAccount pertence obrigatoriamente a exatamente uma Company e uma Branch.
- Branch deve pertencer à mesma Company.
- Tipo: PAYABLE ou RECEIVABLE.
- Partner é global; PAYABLE exige SUPPLIER e RECEIVABLE exige CUSTOMER.
- Partner inativo não pode ser usado em novos lançamentos.
- Category e CostCenter são Company-scoped e inativos não podem ser usados em novos lançamentos.
- Estados persistidos globais: DRAFT, PENDING_APPROVAL, APPROVED, SETTLED, CANCELLED.
- FUNC-005 cria apenas em DRAFT.
- FinancialAccount é aggregate root com 1..N Installments.
- Cada Installment possui ao menos installmentNumber, dueDate e amount.
- installmentNumber > 0, dueDate obrigatória, amount > 0.
- Soma das parcelas deve ser igual ao valor total.
- Valores monetários usam BigDecimal / NUMERIC(19,2).
- Vencimentos em sábado/domingo são permitidos sem ajuste automático.

## Fora do escopo

Approval, rejection, cancellation, settlement, payment, receipt, reversal,
FinancialMovement, balance, overdue derivation, renegotiation, history,
cash flow, dashboard, reports, authentication/authorization, Kafka/Redis/outbox.

## Decisões que o Plan deve fechar

- campos mínimos exatos;
- Partner/Category/CostCenter requiredness;
- description/reference;
- Installment persistent ID;
- parcelas explícitas vs geração;
- money scale/rounding;
- BankAccount/PaymentMethod agora ou somente settlement;
- REST/list representation;
- V8.
