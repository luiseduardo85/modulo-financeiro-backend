# UC-008 — Liquidar Parcela

Permissão conceitual: `CONTA_LIQUIDAR`. A integração de identidade e autorização
permanece deferida; FUNC-007 não aceita ator do cliente nem afirma que essa
permissão já é tecnicamente aplicada.

Liquidação ocorre por Installment de FinancialAccount `APPROVED`. Valor > 0 e
menor ou igual ao saldo derivado. `PAYABLE` gera `PAYMENT`; `RECEIVABLE` gera
`RECEIPT`. Liquidação parcial é permitida e não cria status principal. Quando
todas as Installments alcançam saldo zero, a FinancialAccount muda para
`SETTLED`.

`movementDate` é `LocalDate`, obrigatório, sem default nem restrição de passado
ou futuro. BankAccount e PaymentMethod são obrigatórios, Company-scoped e devem
estar ativos; BankAccount de Branch específica deve corresponder à Branch da
FinancialAccount.

API: POST `/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/installments/{installmentId}/settlements`.

Exige `Idempotency-Key` com operação `SETTLE_INSTALLMENT`. A resposta estável
representa somente a FinancialMovement imutável criada. Reversal permanece fora
de FUNC-007.
