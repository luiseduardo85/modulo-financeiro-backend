# Finalizar FUNC-007

Leia AGENTS.md, MASTER CONTEXT e o último diff. Não push.

Corrija:
1. Transformar FinancialMovement de record público em `final class` imutável com construtor privado; somente `create(...)` e `rehydrate(...)`.
2. Teste PostgreSQL real via SettleInstallment proxied: falha no INSERT de movement depois de claim+version increment deve rollback status/version/movement/idempotency.
3. Teste PostgreSQL real: permitir INSERT de movement e falhar no update/flush final do FinancialAccount; rollback total, movement desaparece, status APPROVED, version anterior, claim ausente.
4. Application tests: BankAccount cross-company, PaymentMethod cross-company, optimistic conflict translation, movementDate null, bankAccountId null/0/negativo, paymentMethodId null/0/negativo, candidate-first sem TECH-010/repositories.
5. Domain: todos IDs externos null/0/negativo, data passada/futura aceitas.
6. MVC: replay com mesmo Location, amount 100.501=>422, trailing zeros válidos, body IDs bounds, rejeitar originalMovementId/reversalId/reversalAmount/reversed.
7. PostgreSQL scoped replay: wrong company/account/installment => empty.
8. Corrigir `docs/database/logical-model.md`.
9. Canonicalizar rotas técnicas futuras em UC-004, UC-007, UC-009, UC-010 sem implementar features.

Preservar concorrência atual, TECH-010/fingerprint, response estável, V10, V1-V9 imutáveis, sem reversal/auth/actor/balance persistido.

Faça review final autônomo. Corrija findings objetivos.
Valide spotless/test/package/verify/diff-check.
Se tudo passar, commit somente FUNC-007:
`feat: implement installment settlement`
Não push.
