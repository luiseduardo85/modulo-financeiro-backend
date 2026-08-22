# FUNC-009 — Financial Account History

Fluxo autônomo completo. Branch `func/func-009-financial-history`. Não push.

Objetivo: histórico/audit trail financeiro sem event sourcing.

Regras:
- histórico append-only;
- criação da FinancialAccount deve finalmente ter evidência histórica;
- ApprovalRequest/Decision e FinancialMovement já são evidências próprias: não duplicar cegamente;
- actorId somente se houver trusted actor real naquele fluxo; nunca do cliente;
- company scope;
- ordenação determinística;
- sem update/delete;
- timestamps técnicos UTC se necessários.

Planeje se o endpoint deve compor timeline a partir de HistoryEntry + Approval + Movement, ou persistir somente eventos não cobertos por entidades específicas.
Não invente tipos sem docs.

API provável:
GET `/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/history`

Crie V12 se necessário. V1-V11 imutáveis.
Teste criação, approval/rejection, settlement/reversal, cross-company, ordering, append-only, rollback.
Review, validate, commit:
`feat: implement financial account history`
