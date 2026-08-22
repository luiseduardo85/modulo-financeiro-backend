# FUNC-011 — Dashboard MVP

Fluxo autônomo completo. Branch `func/func-011-dashboard`. Não push.

Objetivo: endpoints mínimos para Dashboard/summary, sem BI genérico.

Leia telas/docs e implemente somente métricas necessárias.
Possíveis, se suportadas:
- total a pagar/receber;
- vencido a pagar/receber;
- realizado;
- fluxo projetado;
- distribuição por categoria;
- próximos vencimentos.

Regras:
- projections SQL;
- company/branch scoped;
- partial/reversal/cancelled tratados corretamente;
- OVERDUE derivado;
- sem aggregate loading/N+1;
- sem métricas persistidas/materialized views prematuras.

API preferida: poucos endpoints; avaliar GET `/api/v1/companies/{companyId}/dashboard`.
Índice/migration apenas se justificado (V14 se necessária).
Review, validate, commit:
`feat: implement financial dashboard`
