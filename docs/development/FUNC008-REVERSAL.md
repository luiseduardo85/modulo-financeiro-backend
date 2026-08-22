# FUNC-008 — Reversal

Fluxo autônomo: PLAN -> IMPLEMENT -> TEST -> REVIEW -> CORRECT -> VALIDATE -> COMMIT.
Não push. Branch `func/func-008-reversal`. Pré-condição: FUNC-007 commitado.

Objetivo: reversal append-only de FinancialMovement.

Regras:
- nunca apagar/editar movimento efetivo;
- reversal é novo movimento e referencia original;
- partial reversal permitido;
- reversal amount >0 e <= saldo ainda reversível do original;
- nunca reverter um reversal;
- pode reabrir SETTLED -> APPROVED quando volta a existir saldo;
- TECH-010 obrigatório;
- concorrência não pode permitir over-reversal;
- auth/actor permanece adiado.

Planeje primeiro a representação mínima. Preferência, se docs não contradisserem:
- evoluir FinancialMovement com tipos PAYMENT, RECEIPT e tipos de reversal explícitos;
- adicionar `originalMovementId` apenas onde necessário;
- manter movementDate como business date;
- não duplicar histórico genérico.

Teste crítico:
original=100, reversals concorrentes 60+60 => nunca 120.

TECH-010:
operation `REVERSE_FINANCIAL_MOVEMENT`.
Fingerprint versionado incluindo companyId/account/installment/originalMovementId/amount/date e demais campos realmente semânticos.

API preferida:
POST `/api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/installments/{installmentId}/settlements/{movementId}/reversals`

Crie V11. V1-V10 imutáveis.
Sem cascade, sem delete/update.
Testes Domain/Application/MVC/PostgreSQL, rollback, same-key, over-reversal, account reopen.
Review próprio, corrija findings, valide.
Commit:
`feat: implement financial movement reversal`
