# UC-009 — Estornar Movimentação

Permissão conceitual: `CONTA_ESTORNAR`. A integração de identidade e
autorização permanece deferida; FUNC-008 não aceita ator do cliente nem afirma
que essa permissão já é tecnicamente aplicada.

Estorno (reversal) referencia a FinancialMovement original por
`originalMovementId`, pode ser parcial e a soma dos reversals nunca supera o
valor original. Um reversal nunca referencia outro reversal. A FinancialAccount
deve estar `APPROVED` ou `SETTLED`; se estava `SETTLED`, o reversal reabre
saldo e a conta volta para `APROVADA`.

BankAccount e PaymentMethod são obrigatórios, Company-scoped e devem estar
ativos; BankAccount de Branch específica deve corresponder à Branch da
FinancialAccount. `movementDate` é `LocalDate`, obrigatório, sem default nem
restrição de passado ou futuro.

API: POST
`/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/installments/{installmentId}/settlements/{movementId}/reversals`.

Exige `Idempotency-Key` com operação `REVERSE_FINANCIAL_MOVEMENT`. A resposta
estável representa somente a FinancialMovement de reversal imutável criada,
incluindo `originalMovementId`.
