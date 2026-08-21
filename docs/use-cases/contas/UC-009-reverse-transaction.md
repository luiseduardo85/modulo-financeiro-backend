# UC-009 — Estornar Movimentação

Permissão: `CONTA_ESTORNAR`.

Estorno referencia original, pode ser parcial e a soma não supera o original. Se reabrir saldo em QUITADA, volta para APROVADA.

API: POST `/api/v1/contas/{contaId}/movimentacoes/{movimentacaoId}/estornar`.
