# State Machine — FinancialAccount

NOVO -> DRAFT

DRAFT -> PENDING_APPROVAL
DRAFT -> APPROVED (sem fluxo de aprovação)

PENDING_APPROVAL -> APPROVED (aprovar)
PENDING_APPROVAL -> DRAFT (rejeitar)

APPROVED -> CANCELLED (cancelar sem movimentações + justificativa)
APPROVED -> SETTLED (todas parcelas com saldo zero)

SETTLED -> APPROVED (estorno que reabre saldo)

FUNC-006 implementa as quatro transições de aprovação. FUNC-007 implementa
`APPROVED -> SETTLED`, depois de a Application provar por saldo derivado
que todas as Installments estão zeradas. FUNC-008 implementa
`SETTLED -> APPROVED` via reversal, sempre que a FinancialAccount estava
`SETTLED` no momento do reversal — um reversal bem-sucedido sempre reabre
saldo em pelo menos uma Installment. Cancelamento permanece requisito de
slice futuro.

Estados não persistidos como principal:
VENCIDA, REJEITADA, PARCIALMENTE_LIQUIDADA, ESTORNADA.

Clientes da API não alteram status diretamente.
