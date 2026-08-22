# Codex Plan — FUNC-005 Financial Account

```text
Read AGENTS.md first.

We are planning FUNC-005 — Financial Account.

PLAN ONLY. Do not modify files yet.

Inspect current Git state and read source-of-truth docs in precedence order:
1. ADRs
2. Business Rules
3. Domain Model
4. Use Cases
5. API Contracts
6. Existing code

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
- docs/backlog/issues/FUNC-005-financial-account.md

Inspect completed Company/Branch, Partner, Category/CostCenter,
BankAccount/PaymentMethod, TECH-010 idempotency, Flyway and API conventions.

Git implementation branch after approval:
func/func-005-financial-account
Do not create it during PLAN.

1. Aggregate
FinancialAccount is aggregate root with 1..N Installment.
No FinancialMovement yet.
Domain framework-free; no generic Base* abstractions.

2. Type
FinancialAccountType = PAYABLE, RECEIVABLE.
Persist strings, never ordinals.
Prefer one aggregate/resource rather than duplicated payable/receivable models
unless canonical docs explicitly require separation.

3. Ownership
Mandatory immutable scalar companyId and branchId.
Branch must belong to same Company.
Reuse non-leaking companyId + branchId validation.
No external-aggregate @ManyToOne navigation.

4. Partner
Determine whether partnerId is mandatory.
If mandatory:
PAYABLE -> active Partner with SUPPLIER role.
RECEIVABLE -> active Partner with CUSTOMER role.
Both roles valid for either.
Partner remains global.
Distinguish missing, inactive and wrong-role behavior; propose stable errors if
docs do not already define them.

5. Category
Determine requiredness.
If present/required: same Company and active.
Cross-company must not leak.

6. CostCenter
Determine requiredness.
If optional, precisely define null semantics.
If present: same Company and active.
No rateio.

7. BankAccount / PaymentMethod
Determine whether these belong on FinancialAccount creation or only on future
settlement/movement.
Do not include merely because catalogs exist.
If settlement-specific, explicitly defer both.

8. Exact FinancialAccount fields
Evaluate only documented fields:
id, companyId, branchId, type, partnerId, categoryId, costCenterId,
totalAmount, status, installments.
Evaluate description, notes, documentNumber/externalReference, issueDate,
competenceDate, aggregate-level dueDate.
For every field state requiredness and justification.
Do not add audit/settlement/balance/version/metadata fields.

9. Status
Creation = DRAFT.
Clients never send status.
No transitions in this FUNC.
Do not persist REJECTED, OVERDUE, PARTIALLY_SETTLED, REVERSED.

10. Money
BigDecimal / NUMERIC(19,2), no float/double.
totalAmount > 0; installment amount > 0.
Define >2-decimal behavior: reject or normalize according to docs.
Do not invent rounding mode.
Explicit installment sum must equal total exactly after approved scale policy.

11. Installment
Determine exact model, likely:
id (evaluate persistent need),
installmentNumber,
dueDate,
amount.
At least one.
Number >0 and unique within account.
Due date required.
Amount >0.
Evaluate contiguous numbering; do not assume.
Stable Installment ID is likely useful for later settlement, but justify.

12. Installment creation
Compare explicit installments vs backend-generated schedule.
Do not generate automatically without documented frequency/date/rounding rules.
Prefer explicit installments if docs are silent.
Weekend due dates valid.

13. Due date source
Determine whether FinancialAccount itself needs dueDate.
Avoid duplicate source of truth if installments own due dates.

14. Rounding residual
If installments are explicit, explain that residual-to-last-installment applies
only to future automatic split generation unless docs say otherwise.
Do not silently rewrite client amounts.

15. Validation order
Follow established philosophy:
construct/validate Domain candidate first as far as supplied data allows,
then repository-dependent Company/Branch/Partner/Category/CostCenter checks,
then save.
Define deterministic error precedence.

16. Reference validation
Company exists.
Branch exists in company scope.
Partner exists globally, active, correct role.
Category same Company + active.
CostCenter same Company + active if applicable.
BankAccount/PaymentMethod only if section 7 includes them.

17. Repository
Specific FinancialAccountRepository, likely:
save
findByCompanyIdAndId
findPageByCompanyId
Aggregate save should persist account + installments atomically.
No Spring Data types inward.

18. JPA
Separate FinancialAccountJpaEntity and InstallmentJpaEntity.
External references as scalar FKs.
Parent-child JPA relation may be justified inside aggregate; explain cascade
persist/merge and avoid orphanRemoval/delete cascade unless required.
No broad cascades.

19. Flyway
V1-V7 immutable. Plan V8.
Likely tables "financialAccount" and "installment".
Specify all columns/constraints/FKs/indexes exactly.
Installment likely:
id BIGINT identity
financialAccountId BIGINT NOT NULL
installmentNumber
dueDate DATE
amount NUMERIC(19,2)
Evaluate UNIQUE(financialAccountId, installmentNumber).
Index companyId for scoped FinancialAccount lists and financialAccountId for
Installment lookup. No speculative indexes.

20. REST
Plan:
POST /api/v1/companies/{companyId}/financial-accounts
GET  /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}
GET  /api/v1/companies/{companyId}/financial-accounts

Determine whether one resource with `type` is canonical; prefer this unless docs
require separate payable/receivable routes.
Create request must reject id/companyId/status/settlement fields/unknown fields.
Propose exact request/response JSON.
POST 201 + Location. GET/list 200.
Dates YYYY-MM-DD, money JSON numbers -> BigDecimal.

21. Idempotency
TECH-010 exists.
Determine from docs whether FinancialAccount creation needs Idempotency-Key.
Do not assume.
If TECH-010 is intended only for settlement/reversal, explicitly defer.

22. Errors
Reuse current infrastructure.
Propose FINANCIAL_ACCOUNT_NOT_FOUND and only necessary business errors.
Evaluate inactive Partner/Category/CostCenter, wrong Partner role, installment
total mismatch.
Avoid excessive new codes if VALIDATION_ERROR is canonical.

23. Pagination/list
Reuse page0,size20,min1,max100.
Determine minimal sort whitelist from actual model.
Do not expose arbitrary JPA properties.
Do not add filters/search unless docs already require them.
Be cautious with dueDate sorting for multi-installment accounts.

24. GET/list representation
GET by ID likely includes installments.
For list, decide full installments vs summary DTO.
Avoid N+1.
Explain repository/fetch approach.

25. Domain tests
Cover:
valid PAYABLE/RECEIVABLE structure;
positive IDs;
type required;
totalAmount positive;
initial DRAFT;
installment list nonempty;
number positive/unique;
dueDate required;
weekend accepted;
amount positive;
sum equals total;
collection defensively immutable;
money scale behavior.

26. Application tests
Cover:
PAYABLE + SUPPLIER;
RECEIVABLE + CUSTOMER;
Partner with both;
missing Company;
missing/cross-company Branch;
missing/inactive/wrong-role Partner;
missing/cross-company/inactive Category;
CostCenter cases according to requiredness;
structural validation before repository calls;
save only after all validations;
scoped get/list.

27. MVC tests
Cover:
201 + Location;
PAYABLE/RECEIVABLE;
explicit installments;
money/date serialization;
strict/prohibited fields;
stable errors;
get/list;
pagination/sort;
traceId;
Idempotency-Key behavior according to section 21.

28. PostgreSQL IT
PostgreSQL 16 Testcontainers, V1-V8, Hibernate validate.
Prove identities, scalar FKs, aggregate save/reload, multiple installments,
NUMERIC(19,2), DATE, installment uniqueness if approved, scoped list/pagination,
FK delete protection, isolated cleanup. No H2.

29. Documentation
Use canonical English technical names:
FinancialAccount
Installment
FinancialAccountType
FinancialAccountStatus
financialAccountId
installmentId
partnerId
categoryId
costCenterId
companyId
branchId
PAYABLE
RECEIVABLE
Keep unrelated Portuguese explanatory prose.
Document explicit exclusions of approval/settlement/movement/reversal.

30. Spotless
Do not change configuration.
Implementation must pass spotless:apply and spotless:check.

31. Exact files
List exact files expected under Domain, Application, Persistence, REST,
migration, tests, shared error/bootstrap and documentation.

32. Blocking decisions
Explicitly surface:
- Partner mandatory?
- Category mandatory?
- CostCenter mandatory?
- description/reference fields?
- BankAccount/PaymentMethod now or settlement?
- explicit installments?
- Installment persistent ID?
- contiguous numbering?
- money scale/rounding?
- list full installments or summary?

Do not silently resolve unsupported material decisions.

33. Scope exclusions
No approval, rejection, cancellation, settlement, payment, receipt, reversal,
FinancialMovement, balance, overdue derivation, renegotiation, history,
cash flow, dashboard, reports, auth, Kafka, Redis, outbox.

34. Output
Return:
1. doc findings/conflicts
2. blockers
3. exact Domain model
4. invariants
5. reference validation
6. use cases
7. repository
8. JPA
9. V8
10. REST
11. errors
12. pagination/list
13. idempotency
14. tests
15. files
16. Git workflow

Finish with:
PLAN STATUS: READY FOR APPROVAL
or
PLAN STATUS: BLOCKED

Do not modify files.
```
