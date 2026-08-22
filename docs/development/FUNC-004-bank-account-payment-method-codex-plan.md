# Codex Plan — FUNC-004 Bank Account / Payment Method

```text
Read AGENTS.md first.

We are planning:

FUNC-004 — Bank Account / Payment Method.

This is PLAN ONLY.
Do not modify files yet.

Before planning:

1. Inspect the current Git state.
2. Confirm FUNC-003 is finalized in the current baseline.
3. Read AGENTS.md and relevant documentation in precedence order.
4. Inspect current Flyway history, Spotless configuration, and existing Company/Branch patterns.

Read at minimum:

- AGENTS.md
- docs/requirements/business-rules.md
- docs/domain/*
- docs/use-cases/*
- docs/api/*
- docs/database/*
- docs/architecture/backend-architecture.md
- docs/architecture/persistence.md
- docs/architecture/transactions.md
- docs/architecture/testing.md
- docs/backlog/issues/FUNC-004-bank-account-payment-method.md

0. Git workflow
Plan implementation branch:
func/func-004-bank-account-payment-method

Do not create the branch during PLAN.
At implementation time create/switch safely, preserve history, do not push automatically.

1. BankAccount confirmed rules
- belongs to exactly one Company;
- not shared across Companies;
- may be usable by Branches of same Company;
- inactive cannot be used for future new movements;
- historical references remain valid;
- no physical delete for inactivation;
- companyId mandatory/immutable.

2. BankAccount minimum domain model
Determine smallest useful entity.
Evaluate only if justified:
id, companyId, name, bankCode/bankName, agency, accountNumber, accountDigit,
accountType, branchId restriction, active.

For every proposed field identify supporting documentation or mark as a proposed
decision needing approval.

Do not automatically add balances, PIX, IBAN/SWIFT, CNAB, credentials,
reconciliation, accounting data, timestamps, audit fields, version, soft delete.

3. Branch relation
Inspect current canonical rule.
If branchId is optional, define exact null semantics and non-null restriction semantics.
Validate same-company branch ownership.
If docs are insufficient, mark BLOCKING rather than inventing behavior.
Prefer scalar IDs and real DB constraints where feasible; no @ManyToOne solely for ownership.

4. Uniqueness
Do not assume bank+agency+account uniqueness or name uniqueness without source rule.

5. BankAccount lifecycle
Evaluate boolean active, creation active=true, deactivate only if supported.
No reactivation/delete unless documented.
Inactive remains retrievable/listable.

6. PaymentMethod ownership
Resolve current canonical scope from documentation:
global vs Company-scoped.
Do not infer.
This is blocking if contradictory.
Do not add PAYABLE/RECEIVABLE type.

7. PaymentMethod minimum model
Evaluate only justified fields:
id, companyId if scoped, name, active.
Do not add code, settlement rules, fees, cards, PIX/boleto specifics,
accounting config, timestamps, version, metadata.

8. PaymentMethod lifecycle
Only model active/deactivate if documented.
No delete/reactivation unless documented.

9. Domain
Framework-free. No generic catalog base class.

10. Application/repositories
Use context-specific ports/use cases only.
Likely BankAccount:
Create/Get/ListByCompany/Deactivate if approved.
Likely PaymentMethod:
Create/Get/List according to scope/Deactivate if approved.
No update/delete automatically.
Company-scoped resources use scoped lookups and non-leaking cross-company behavior.
If Branch involved, validate Branch belongs to Company.

11. Persistence
Separate JPA entities.
Long/BIGINT identity, explicit nullability/lengths, quoted camelCase,
scalar IDs, no broad cascade, no ordinal enums, Hibernate validate only.

12. Flyway
V1-V6 immutable.
Plan next migration starting V7.
Define exact tables/columns/constraints/indexes only after model decisions.
Likely technical names:
"bankAccount"
"paymentMethod"
Verify canonical docs first.
No ON DELETE CASCADE without explicit rule.

13. REST
Plan explicit English routes.
Likely BankAccount:
/api/v1/companies/{companyId}/bank-accounts

PaymentMethod route depends on ownership:
Company-scoped -> /api/v1/companies/{companyId}/payment-methods
Global -> /api/v1/payment-methods

Do not decide ownership by guess.
Use strict request DTOs locally, no global ObjectMapper policy.
Create 201 + Location.

14. Errors
Reuse existing infrastructure.
Potential codes only after model/scope is confirmed:
BANK_ACCOUNT_NOT_FOUND
PAYMENT_METHOD_NOT_FOUND
Reuse COMPANY_NOT_FOUND / BRANCH_NOT_FOUND where appropriate.
No duplicate errors without uniqueness.

15. Pagination
Reuse page=0,size=20,min1,max100,default id,asc, explicit whitelist.
No speculative search/filter.

16. Historical preservation
Inactive catalog values remain resolvable historically.
Do not implement FinancialAccount yet.

17. Hermetic tests
Plan Domain/Application/MVC tests for ownership, create/get/list, cross-company,
Branch restriction if approved, lifecycle, strict requests, pagination/sort,
errors, traceId, no Idempotency-Key.

18. PostgreSQL tests
Use PostgreSQL 16 Testcontainers + real Flyway + Hibernate.
Prove migration, identities, FKs, ownership isolation, same-company Branch
constraints if applicable, no cascade, lifecycle persistence, pagination/sorts.
No H2.

19. Spotless
Already established. Do not reconfigure unless a defect exists.
All Java must pass spotless:apply/check.
Do not create a new repository-wide formatting diff if baseline is compliant.

20. Git implementation workflow
When approved:
- ensure baseline safe;
- create/switch func/func-004-bank-account-payment-method;
- implement;
- review/fix;
- full validation;
- organize commits;
- do not push automatically.

21. Scope exclusions
No balances, reconciliation, Open Finance, PIX integration, CNAB, boleto,
gateways, FinancialAccount, Installment, FinancialTransaction, approval,
settlement, reversal, auth, Kafka, Redis, outbox, generic catalog framework.

22. Files
List exact expected files grouped by BankAccount, PaymentMethod, shared, migration, docs.

23. Documentation conflicts/blockers
Explicitly identify and DO NOT silently resolve:
- BankAccount Branch binding/restriction;
- PaymentMethod ownership;
- active/inactive semantics;
- uniqueness;
- Portuguese vs English technical naming;
- older discovery rules superseded by later domain decisions.

Use precedence:
ADRs
Business Rules
Domain Model
Use Cases
API Contracts
Existing code

If a material decision remains unsupported/contradictory, mark BLOCKING FOR IMPLEMENTATION.

Do not modify files yet.
```
