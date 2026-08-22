# UC-006 — Rejeitar Conta Financeira

Permissão: `CONTA_APROVAR`.

`PENDING_APPROVAL` -> `DRAFT`. Justificativa obrigatória, normalizada e persistida em `ApprovalDecision`, com até 500 caracteres. Auto-rejeição é permitida quando o ator é elegível. `ApprovalRequest` anterior permanece `REJECTED`; novo envio cria outro ciclo.

API: POST `/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/reject`.
