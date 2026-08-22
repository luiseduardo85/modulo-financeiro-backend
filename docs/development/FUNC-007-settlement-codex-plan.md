# Codex Plan — FUNC-007 Settlement / Payment / Receipt

Read AGENTS.md first.

PLAN ONLY.
Do not modify files.
Do not create branch.
Do not commit.
Do not push.

Inspect current Git state and confirm the approved baseline through FUNC-006 and Flyway V1-V9.
Read ADRs, Business Rules, Domain Model, Use Cases, API Contracts, database/architecture docs,
TECH-010, FinancialAccount/Installment, BankAccount, PaymentMethod and approval concurrency code.

Plan implementation branch later:
func/func-007-settlement

## 1. Scope
Implement settlement only.
PAYABLE -> PAYMENT.
RECEIVABLE -> RECEIPT.
No reversal, cancellation, renegotiation, generic history, cash flow, dashboard,
reports, external auth, Kafka, Redis or outbox.

## 2. FinancialMovement
Derive the minimum exact model. Evaluate:
- id
- installmentId
- financialAccountId if justified
- companyId if justified
- type: PAYMENT / RECEIPT
- amount
- movementDate
- bankAccountId
- paymentMethodId

Do not add reversal fields yet unless a higher-precedence source requires them.
Do not invent memo/description/reference fields.

Explicitly decide whether FinancialMovement is a child of FinancialAccount or a
separate aggregate/entity referencing Installment. Consider unbounded history,
future reversal, concurrency, aggregate size and query behavior.

## 3. Settlement target
Settlement targets exactly one Installment.
Plan a company-scoped lookup proving:
company -> financialAccount -> installment.
Cross-company/cross-account installment must not leak.

## 4. Eligible FinancialAccount statuses
Derive exact rule. Expected:
APPROVED = settleable.
DRAFT, PENDING_APPROVAL, CANCELLED, SETTLED = not settleable.
Do not assume if docs contradict.

## 5. Partial settlement and balance
Partial settlement allowed.
No persisted PARTIALLY_SETTLED.
Balance is derived, not client-editable.

Define:
- settled amount per Installment
- remaining balance per Installment
- total remaining FinancialAccount balance

Prefer sums/projections over loading unbounded movement collections.
Avoid N+1.
Do not persist redundant balance unless clearly justified.

FinancialAccount becomes SETTLED only when all Installments have zero remaining balance.

## 6. Money
BigDecimal only.
NUMERIC(19,2).
At most two effective decimal places.
Trailing zeros valid.
No silent rounding.
Movement amount required, >0, <= remaining balance.

## 7. BankAccount
Resolve whether mandatory.
If used for a new settlement, expected rules:
- exists in FinancialAccount Company;
- active;
- branchId null => usable by all company branches;
- branchId non-null => must equal FinancialAccount.branchId.
Define exact errors.
Historical inactive references remain valid.

## 8. PaymentMethod
Resolve whether mandatory.
Expected new-settlement rules:
- exists in FinancialAccount Company;
- active.
Same method can serve PAYMENT or RECEIPT.
Define exact errors.

## 9. Movement date
Resolve exact field name and semantics.
Prefer one neutral field for PAYMENT and RECEIPT.
Determine LocalDate vs Instant, required/default behavior, future/past acceptance.
Do not invent "today" default. Mark BLOCKING if docs are silent.

## 10. Concurrency / overpayment
This is CRITICAL.

Example:
Installment amount 100, current settled 0.
Two concurrent settlements of 60 + 60 must never both commit.

Existing FinancialAccount @Version may NOT be enough if partial settlements do not
change account status/version.

Evaluate and choose the simplest correct mechanism:
- force FinancialAccount version increment on every settlement;
- Installment @Version;
- atomic remaining-balance update;
- pessimistic lock on target Installment;
- another justified strategy.

Explain why the chosen strategy prevents overpayment and lost updates.

Also analyze 60 + 40 concurrent settlement. A correct serialized design may allow
both and end at zero.

## 11. TECH-010 idempotency
Strongly evaluate settlement as a REQUIRED TECH-010 consumer.

If approved:
- Idempotency-Key required;
- existing key validation reused;
- scope = companyId + operation + key;
- fingerprint includes semantic request fields only;
- claim + settlement mutation + completion in one transaction;
- same key/same fingerprint replays authoritative success;
- same key/different fingerprint -> 409;
- missing/invalid key uses existing TECH-010 errors.

Do not create a second idempotency implementation.

Define exact operation name, fingerprint schema version/order and resultReference.

Likely fingerprint fields:
operation
companyId
financialAccountId
installmentId
amount canonical
movementDate
bankAccountId
paymentMethodId

Include explicit null markers for optional fields.

## 12. Idempotent response
Define resultReference, likely FinancialMovement ID.
On replay, reload authoritative result.

Choose a stable response that may include:
- movement id/type/amount/date;
- installment id;
- installment amount;
- settledAmount;
- remainingBalance;
- FinancialAccount status.

Avoid requiring full movement-history loading.

## 13. Transaction flow
Plan exact order, preserving candidate-first validation.

Candidate:
1. structurally validate request/value objects;
2. claim idempotency key;
3. scoped account/installment load;
4. verify settleable status;
5. validate BankAccount;
6. validate PaymentMethod;
7. enter concurrency-safe settlement boundary;
8. derive current balance;
9. reject overpayment;
10. create FinancialMovement;
11. persist movement;
12. determine whether all installments are fully settled;
13. if yes, FinancialAccount -> SETTLED;
14. persist/version parent as required;
15. complete idempotency record;
16. commit once.

Explain exact ordering against TECH-010 current implementation.

## 14. Domain transition
No generic status setter.
Plan an explicit FinancialAccount transition to SETTLED that cannot be called while
any installment still has balance, with Application providing derived settlement state
as appropriate.
Do not implement SETTLED -> APPROVED reopening; reversal owns that later.

## 15. FinancialMovement immutability
No DELETE endpoint.
No update endpoint.
No soft delete.
Future reversal creates a new movement.

## 16. Repository ports
Define exact contextual ports. No Spring Data types inward.
Likely FinancialMovementRepository plus any safe settlement/balance query or
mutation-oriented FinancialAccount/Installment repository operation.
Avoid generic CRUD.

## 17. REST
Prefer one canonical settlement action because account type determines movement type.

Evaluate:
POST /api/v1/companies/{companyId}/financial-accounts/{financialAccountId}/installments/{installmentId}/settlements

Client must NOT choose PAYMENT/RECEIPT type.

Request likely:
- amount
- movementDate
- bankAccountId
- paymentMethodId

subject to approved decisions.

Reject unknown/prohibited:
id, companyId, financialAccountId, installmentId, movement type, status,
balance, settledAmount, reversal fields.

Define status code, Location semantics if 201, replay behavior and response shape.

## 18. Errors
Propose minimal stable errors such as:
FINANCIAL_ACCOUNT_NOT_SETTLEABLE
INSTALLMENT_NOT_FOUND
INSTALLMENT_ALREADY_SETTLED
SETTLEMENT_AMOUNT_EXCEEDS_BALANCE
BANK_ACCOUNT_NOT_FOUND
BANK_ACCOUNT_INACTIVE
BANK_ACCOUNT_BRANCH_NOT_ALLOWED
PAYMENT_METHOD_NOT_FOUND
PAYMENT_METHOD_INACTIVE
SETTLEMENT_CONFLICT

Reuse existing FinancialAccount, validation, malformed request and TECH-010 errors.
Avoid unnecessary fragmentation.

## 19. V10
V1-V9 immutable.
Plan V10__create_financial_movement.sql.

Specify exact:
- columns
- quoted camelCase names
- enum CHECK
- positive amount CHECK
- FKs
- no cascade
- indexes for installment movement sum, account settlement checks and future reversal lookup.

Do not add reversal columns prematurely without justification.

## 20. Database integrity
Postgres can enforce:
- positive amount;
- allowed movement type;
- FKs;
- no cascade.

Application/concurrency must enforce:
- remaining balance;
- PAYABLE/PAYMENT and RECEIVABLE/RECEIPT;
- bank same company/branch;
- payment method same company;
- active state;
- account status.

Do not overclaim DB guarantees.

## 21. Tests — Domain
Plan:
- FinancialMovement invariants;
- PAYMENT/RECEIPT mapping;
- money precision;
- positive amount;
- explicit SETTLED transition;
- no generic movement mutation/deletion.

## 22. Tests — Application
Plan:
- PAYABLE -> PAYMENT;
- RECEIVABLE -> RECEIPT;
- partial settlement;
- exact final installment settlement;
- account remains APPROVED while any installment has balance;
- account -> SETTLED only when all are zero;
- invalid account statuses;
- overpayment;
- BankAccount company/branch/active;
- PaymentMethod company/active;
- candidate-first validation;
- idempotency first call/replay/conflict/missing key if approved;
- concurrency conflict/no overpayment.

## 23. Tests — MVC
Plan:
- canonical POST route;
- Idempotency-Key contract;
- request strictness;
- positive IDs;
- malformed amount/date;
- movement type cannot be supplied;
- PAYMENT/RECEIPT derived behavior;
- partial/full response;
- stable errors/traceId;
- replay behavior.

## 24. Tests — PostgreSQL 16
Plan:
- V10;
- Hibernate validate;
- movement identity;
- NUMERIC(19,2);
- business date type;
- FKs/checks;
- no cascade;
- multiple movements per Installment;
- sum/balance query;
- account SETTLED atomicity;
- idempotency contention/replay;
- FK-safe deterministic cleanup.

Mandatory real concurrency test:
Installment=100, two concurrent settlements 60 and 60.
Never allow total settled=120.

Also analyze/test concurrent 60 + 40 according to chosen locking strategy.

## 25. Auth/history boundaries
External authentication remains deferred.
Do not reuse ApprovalActorContext unless settlement explicitly needs actor identity.
If audit actor is required, surface it as BLOCKING.

FinancialMovement itself is permanent business evidence.
Do not create generic history records.

## 26. Blocking decisions
Explicitly resolve or mark BLOCKING:
1. exact FinancialMovement fields;
2. aggregate ownership;
3. companyId/financialAccountId redundancy;
4. movement date name/type/semantics;
5. BankAccount mandatory?;
6. PaymentMethod mandatory?;
7. inactive BankAccount behavior;
8. inactive PaymentMethod behavior;
9. settleable statuses;
10. balance derivation;
11. overpayment concurrency mechanism;
12. whether every settlement increments a version;
13. TECH-010 required?;
14. fingerprint/version/order;
15. resultReference/replay;
16. REST route;
17. response contract;
18. read endpoints?;
19. V10 schema/indexes;
20. error codes.

Do not implement until approved.

## 27. Expected files
List exact expected files grouped by:
- FinancialAccount Domain changes
- FinancialMovement Domain
- Application/use cases
- repository ports
- persistence/JPA
- REST
- TECH-010 integration changes
- V10
- tests
- shared errors
- canonical docs.

## 28. Output
Return:
1. documentation findings/conflicts
2. blockers
3. proposed FinancialMovement model
4. ownership choice
5. settlement eligibility/state transition
6. balance model
7. BankAccount/PaymentMethod rules
8. movement date
9. concurrency design
10. idempotency design
11. fingerprint
12. transaction flow
13. V10
14. REST
15. response
16. errors
17. tests
18. expected files
19. Git workflow

Finish exactly:
PLAN STATUS: READY FOR APPROVAL
or
PLAN STATUS: BLOCKED

Do not modify files.
