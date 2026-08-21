# UC-004 — Alterar Conta Financeira

Permissão: `CONTA_EDITAR`.

RASCUNHO permite edição normal; PENDENTE_APROVACAO não permite edição direta; vencimento não muda após aprovação nem quando vencido; renegociação gera novo lançamento.

API: PUT `/api/v1/contas/{id}`.
