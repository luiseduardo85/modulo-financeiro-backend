# UC-009 — Estornar Movimentação

Permissão: `CONTA_ESTORNAR`.

Estorno referencia original, pode ser parcial e a soma não supera o original. Se reabrir saldo em QUITADA, volta para APROVADA.

API futura, com implementação deferida ao FUNC-008: POST
`/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/movements/{movementId}/reverse`.
