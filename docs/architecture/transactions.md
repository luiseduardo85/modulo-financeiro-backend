# Transactions

Fronteiras transacionais pertencem aos Application Use Cases.

Operações críticas: aprovação, rejeição, cancelamento, liquidação e estorno.

Uma transação pode abranger agregado, histórico e Outbox.
Controllers não controlam transações de negócio.

Em FUNC-007, claim TECH-010, incremento otimista da FinancialAccount, criação da
FinancialMovement, eventual transição para `SETTLED` e conclusão idempotente
ocorrem na mesma transação Application. Nenhum passo usa `REQUIRES_NEW`.

Em FUNC-008, claim TECH-010, incremento otimista da FinancialAccount, criação
da FinancialMovement de reversal, eventual transição `SETTLED -> APPROVED` e
conclusão idempotente ocorrem igualmente na mesma transação Application. A
FinancialMovement original nunca é apagada ou atualizada; somente uma nova
linha é inserida.

Em FUNC-009, o registro de um evento de histórico (`CREATED` em
`CreateFinancialAccount`, `APPROVED_WITHOUT_WORKFLOW` em
`SubmitFinancialAccountForApproval`) ocorre na mesma transação Application do
efeito que ele evidencia; uma falha no INSERT de
`FinancialAccountHistory` reverte também esse efeito. A consulta de histórico
é somente leitura (`@Transactional(readOnly = true)`) e não cria nem
modifica nenhuma linha.
