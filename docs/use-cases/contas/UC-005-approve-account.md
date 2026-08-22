# UC-005 — Aprovar Conta Financeira

Permissão: `CONTA_APROVAR`.

`PENDING_APPROVAL` -> `APPROVED`. O `ApprovalActor` deve ser elegível por `ApprovalEligibility` para `CONTA_APROVAR` na Company e diferente do `requesterActorId`. Um aprovador é suficiente. A decisão persiste em `ApprovalDecision`; a identidade vem somente de `ApprovalActorContext`.

API: POST `/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/approve`.
