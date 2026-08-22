# FinancialAccount

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

## Estados técnicos
`DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `SETTLED`, `CANCELLED`.

O FUNC-005 cria exclusivamente `DRAFT` e não implementa transições. Contém
uma ou mais `Installment` explícitas; números são positivos e únicos, sem
exigência de continuidade, e a soma monetária deve ser igual a `totalAmount`.
Valores com mais de duas casas decimais efetivas são rejeitados sem arredondar.

O requisito completo de criação inclui o registro de histórico. A persistência
desse histórico foi deliberadamente adiada para o slice dedicado de History;
assim, FUNC-005 cria FinancialAccount e Installments sem histórico, uma lacuna
funcional temporária explicitamente documentada.

## Comportamentos esperados
`enviarParaAprovacao`, `finalizarSemAprovacao`, `aprovar`, `rejeitar`, `cancelar`, `liquidarParcela`, `estornarMovimentacao`.
