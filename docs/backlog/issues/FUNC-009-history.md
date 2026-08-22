# FUNC-009 — Financial Account History

## Objetivo
Implementar histórico/audit trail de FinancialAccount sem event sourcing.

## Regras confirmadas
- Histórico é append-only.
- Criação de FinancialAccount deve finalmente ter evidência histórica.
- ApprovalRequest/Decision e FinancialMovement já são evidências próprias:
  não duplicadas cegamente.
- `actorId` somente quando existe trusted actor real naquele fluxo; nunca do
  cliente.
- Company scope.
- Ordenação determinística.
- Sem update/delete.
- Timestamps técnicos UTC quando necessários para ordenação.

## O Plan fechou
- endpoint compõe timeline a partir de `FinancialAccountHistory` +
  `ApprovalRequest`/`ApprovalDecision` + `FinancialMovement`, em vez de
  persistir um evento genérico para cada ação já evidenciada;
- `FinancialAccountHistory` persiste somente os dois eventos sem evidência
  própria: `CREATED` (sem ator, pois a criação não tem contexto de ator) e
  `APPROVED_WITHOUT_WORKFLOW` (com o ator confiável do submit sem fluxo);
- `createdAt` técnico (`TIMESTAMPTZ DEFAULT now()`) adicionado a
  `approvalRequest`, `approvalDecision` e `financialMovement` para permitir
  ordenação determinística da timeline composta, sem alterar as Entities JPA
  existentes (colunas não mapeadas, preenchidas só pelo DEFAULT do banco);
- leitura da timeline via consultas SQL nativas dedicadas, sem tocar nos
  agregados/Entities de Approval ou FinancialMovement;
- V12 e índice por `financialAccountId`;
- atomicidade evento de histórico + efeito que ele evidencia, na mesma
  transação Application.
