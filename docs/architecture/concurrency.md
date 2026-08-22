# Concurrency

Estratégia inicial: optimistic locking, com `@Version` na persistência quando aplicável.

Conflitos concorrentes financeiros não podem sobrescrever dados silenciosamente.
Conflitos normalmente resultam em HTTP 409.

FUNC-007 usa a versão de persistência da FinancialAccount como token comum de
serialização para toda FinancialMovement criada em qualquer Installment da
conta. Antes de consultar o saldo, executa incremento otimista imediato
equivalente a `OPTIMISTIC_FORCE_INCREMENT`, condicionado à versão carregada.
Falha da condição produz `SETTLEMENT_CONFLICT`; movimento, status e claim
idempotente são revertidos juntos. Installment não ganha `@Version` e saldo não
é persistido.

FUNC-008 reutiliza exatamente o mesmo token de serialização e o mesmo
incremento otimista para toda reversal, antes de calcular o saldo ainda
reversível da movimentação original. Isso impede que dois reversals
concorrentes sobre a mesma FinancialMovement excedam, juntos, o valor
originalmente movimentado. Falha da condição produz o mesmo `SETTLEMENT_CONFLICT`.
