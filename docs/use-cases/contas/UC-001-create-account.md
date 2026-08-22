# UC-001 — Criar Conta Financeira

Permissão: `CONTA_CRIAR`.

Cria `FinancialAccount` válida em `DRAFT`, valida Company, Branch, Partner,
Category, CostCenter opcional, valor e Installments, e persiste o Aggregate de
forma atômica. O requisito final também exige histórico de criação; sua
persistência está explicitamente deferida ao slice dedicado de History.

API no FUNC-005: POST `/api/v1/companies/{companyId}/financial-accounts`.
