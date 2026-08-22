# FUNC-006 — Approval Workflow

## Objetivo
Implementar o workflow inicial de aprovação de FinancialAccount.

## Regras confirmadas
- Estados persistidos: DRAFT, PENDING_APPROVAL, APPROVED, SETTLED, CANCELLED.
- Aprovação é configurável conceitualmente por Company/Branch/type.
- Quando habilitada: DRAFT -> PENDING_APPROVAL -> APPROVED.
- Rejeição exige justificativa e retorna para DRAFT.
- Múltiplos aprovadores podem existir; qualquer um permitido pode aprovar.
- Solicitante não pode aprovar a própria conta.
- Permissão conceitual: CONTA_APROVAR.
- Se aprovação estiver desabilitada, a conta pode seguir de DRAFT para APPROVED conforme regra aprovada.
- Auth externa permanece adiada.

## Fora do escopo
Settlement, payment, receipt, reversal, FinancialMovement, auth externa,
notificações, Kafka/Redis/outbox, dashboard/relatórios.

## Decisões que o Plan deve fechar
- configuração/granularidade de aprovação;
- actor/requester/approver sem auth externa;
- persistência de requester;
- representação de approvers;
- rejeição/justification;
- entidade ApprovalRequest/Decision;
- comportamento quando aprovação está desabilitada;
- repetição/idempotência;
- optimistic locking;
- V9+ e REST actions.
