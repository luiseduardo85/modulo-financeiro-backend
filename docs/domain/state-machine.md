# State Machine — ContaFinanceira

NOVO -> RASCUNHO

RASCUNHO -> PENDENTE_APROVACAO
RASCUNHO -> APROVADA (sem fluxo de aprovação)

PENDENTE_APROVACAO -> APROVADA (aprovar)
PENDENTE_APROVACAO -> RASCUNHO (rejeitar)

APROVADA -> CANCELADA (cancelar sem movimentações + justificativa)
APROVADA -> QUITADA (todas parcelas com saldo zero)

QUITADA -> APROVADA (estorno que reabre saldo)

Estados não persistidos como principal:
VENCIDA, REJEITADA, PARCIALMENTE_LIQUIDADA, ESTORNADA.

Clientes da API não alteram status diretamente.
