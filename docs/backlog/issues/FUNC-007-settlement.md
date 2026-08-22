# FUNC-007 — Settlement / Payment / Receipt

## Objetivo
Implementar liquidação de parcelas de FinancialAccount por FinancialMovement.

## Regras confirmadas
- PAYABLE -> PAYMENT.
- RECEIVABLE -> RECEIPT.
- Liquidação parcial permitida.
- Movimento > 0 e <= saldo restante da Installment.
- FinancialAccount vira SETTLED apenas quando todas as parcelas tiverem saldo zero.
- PARTIALLY_SETTLED não é status persistido.
- BankAccount e PaymentMethod entram nesta fase.
- Money: BigDecimal / NUMERIC(19,2), sem arredondamento silencioso.
- Movimentos efetivos não são apagados.
- Reversal fica para FUNC-008.
- TECH-010 deve ser avaliado fortemente para Settlement.

## O Plan deve fechar
- modelo FinancialMovement;
- data do movimento;
- obrigatoriedade/regras de BankAccount e PaymentMethod;
- balance derivado;
- concorrência/overpayment;
- optimistic locking/locking adicional;
- TECH-010/fingerprint/replay;
- endpoint/response/errors;
- V10 e índices;
- atomicidade movimento + status da conta.
