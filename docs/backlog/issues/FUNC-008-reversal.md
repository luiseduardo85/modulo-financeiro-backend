# FUNC-008 — Reversal

## Objetivo
Implementar reversal (estorno) append-only de FinancialMovement, referenciando
a movimentação original.

## Regras confirmadas
- Movimento efetivo nunca é apagado ou atualizado.
- Reversal é sempre uma nova FinancialMovement referenciando a original por
  `originalMovementId`.
- Reversal parcial é permitido.
- Reversal amount > 0 e <= saldo ainda reversível da original.
- Um reversal nunca reverte outro reversal.
- `FinancialAccount` `SETTLED` pode reabrir para `APPROVED` quando volta a
  existir saldo.
- TECH-010 obrigatório, operação `REVERSE_FINANCIAL_MOVEMENT`.
- Concorrência não pode permitir over-reversal.
- Auth/actor permanece adiado.

## O Plan fechou
- `FinancialMovementType` ampliado com `REVERSAL_PAYMENT`/`REVERSAL_RECEIPT`,
  em vez de uma entidade de reversal separada;
- `originalMovementId` adicionado somente à FinancialMovement existente;
- saldo derivado passa a ser líquido (pagamentos/recebimentos menos reversals);
- mesma serialização otimista da FinancialAccount usada por FUNC-007 é
  reutilizada para reversal;
- TECH-010/fingerprint/replay seguem o mesmo padrão de FUNC-007;
- endpoint aninhado sob o settlement original;
- V11 e índice parcial por `originalMovementId`;
- atomicidade reversal + eventual reabertura da conta.
