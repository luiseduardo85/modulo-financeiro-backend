# UC-010 — Consultar Histórico

Permissão: `CONTA_VISUALIZAR`.

Histórico consultado separadamente do Aggregate, composto a partir de quatro
fontes: `FinancialAccountHistory` (eventos técnicos não evidenciados em
nenhuma outra entidade — criação e aprovação direta sem fluxo),
`ApprovalRequest`/`ApprovalDecision` (submissão e decisão de workflow) e
`FinancialMovement` (liquidação e estorno). Nenhuma dessas fontes é
duplicada; cada uma contribui com os eventos que só ela evidencia.

A timeline é ordenada deterministicamente por um `createdAt` técnico UTC
adicionado a cada fonte, com desempate por fase (criação, depois
submissão/aprovação, depois movimentações) e por ID de origem.

API: GET `/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/history`.

Não exige `Idempotency-Key` (somente leitura). Retorna uma lista ordenada e
não paginada de eventos; cada evento expõe um `type` discriminador e campos
esparsos conforme o tipo. Escopo de Company é validado carregando a
FinancialAccount antes de montar a timeline.
