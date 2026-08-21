# ContaFinanceira

Aggregate Root do núcleo financeiro.

## Invariantes
- empresa obrigatória;
- filial obrigatória e da mesma empresa;
- valor > 0;
- ao menos uma parcela;
- soma das parcelas = valorTotal;
- ciclo de vida alterado por comportamentos, não setters genéricos;
- cancelamento bloqueado após movimentação;
- vencimento protegido após aprovação;
- estorno preserva movimentação original.

## Estados
RASCUNHO, PENDENTE_APROVACAO, APROVADA, QUITADA, CANCELADA.

## Comportamentos esperados
`enviarParaAprovacao`, `finalizarSemAprovacao`, `aprovar`, `rejeitar`, `cancelar`, `liquidarParcela`, `estornarMovimentacao`.
