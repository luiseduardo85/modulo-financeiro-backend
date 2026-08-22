# Codex Plan — FUNC-006 Approval Workflow

```text
Read AGENTS.md first.

We are planning FUNC-006 — Approval Workflow.

PLAN ONLY. Do not modify files yet.

Inspect current Git state and read approval-related source-of-truth docs in precedence order:
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
- docs/backlog/issues/FUNC-006-approval.md
- docs/permissions/* if present

Inspect current FinancialAccount implementation and statuses.

Git branch after approval:
func/func-006-approval
Do not create it during PLAN.

1. State machine
Confirm persisted states:
DRAFT, PENDING_APPROVAL, APPROVED, SETTLED, CANCELLED.
Derive exact approval transitions from docs.
Expected candidates:
DRAFT -> PENDING_APPROVAL
PENDING_APPROVAL -> APPROVED
PENDING_APPROVAL -> DRAFT on rejection
Possibly DRAFT -> APPROVED when approval disabled.
Do not assume others.

2. Approval configuration
Resolve exact granularity and precedence.
Known intent: configurable by Company/Branch/type.
Evaluate a minimum model only if supported:
ApprovalConfiguration
- id
- companyId
- branchId nullable
- financialAccountType
- approvalRequired
Do not add thresholds, levels, escalation, SLA, notifications, timestamps/version without need.
If precedence semantics are undefined, mark BLOCKING.

3. Approvers / actor model
Multiple approvers may exist; any one sufficient; requester cannot approve own.
Inspect whether User/Profile persistence already exists.
Auth is deferred, so do not create a full auth subsystem.
Determine smallest safe model for actor identity and approval eligibility.
Do not trust arbitrary request-supplied approver IDs as authorization proof.
Separate business actor identity from future authentication transport.

4. Permissions
Confirmed conceptual permission: CONTA_APROVAR.
Determine whether to model an application port for actor/permission checks, defer enforcement, or block.
Do not add Spring Security.
Do not hardcode fake authorization.

5. Requester identity
FUNC-005 did not persist requester/createdBy.
Determine where requester must live:
- FinancialAccount field?
- approval request record?
- submit action record?
- other documented model?
This is a high-priority blocker.

6. Approval request/decision persistence
Determine whether FinancialAccount alone is sufficient or a separate ApprovalRequest/ApprovalDecision record is needed.
Evaluate:
- requester identity
- approver identity
- rejection justification
- repeated submission after rejection
- auditability
History persistence is deferred, but required approval business data must not be discarded.

7. Rejection
Confirmed justification required and transition back to DRAFT.
Determine whether justification must be persisted now and where.
Do not accept and discard required business data.
Determine normalization/max length only from docs or mark for approval.

8. Submit use case
Evaluate SubmitFinancialAccountForApproval.
Rules:
- scoped FinancialAccount exists
- current status DRAFT
- if approval required -> PENDING_APPROVAL
- if disabled -> direct APPROVED only if canonical semantics say so
- requester recorded where needed
Determine repeated submit semantics.

9. Approve use case
Evaluate ApproveFinancialAccount.
Rules:
- scoped FinancialAccount exists
- status PENDING_APPROVAL
- actor eligible
- actor != requester
- any valid approver sufficient
- -> APPROVED
Determine repeated approve semantics.

10. Reject use case
Evaluate RejectFinancialAccount.
Rules:
- scoped account
- PENDING_APPROVAL
- actor eligible
- justification required
- -> DRAFT
Determine whether self-rejection is forbidden or only self-approval.
Do not extend the rule without docs.
Determine repeated reject semantics.

11. Approval disabled semantics
Resolve whether:
A. submit action directly performs DRAFT -> APPROVED
or
B. approve action may approve DRAFT when configuration disables workflow.
Choose only from docs.

12. Domain transitions
FinancialAccount Domain should own legal transitions with explicit methods.
No generic setStatus/changeStatus.
Define invalid-transition exceptions.

13. Concurrency
FUNC-006 introduces mutations.
Re-evaluate optimistic locking.
Strongly consider FinancialAccount persistence `version` + JPA @Version + BIGINT column.
Determine whether version belongs only to persistence or Domain.
Cover races: approve/approve, approve/reject, repeated submit.
Avoid last-write-wins.
Do not default to pessimistic locking.

14. Idempotency
Determine whether Submit/Approve/Reject need TECH-010.
Do not assume.
State guards + optimistic locking may be enough.
Settlement/Reversal remain primary high-risk consumers.
Explain exact decision.

15. Ports
Identify exact needed ports:
- ApprovalConfigurationRepository?
- Actor/Permission port?
- ApprovalRequestRepository?
Reuse FinancialAccountRepository.
No generic workflow/authorization engine.

16. Persistence / V9
V1-V8 immutable.
Plan V9 based on resolved model:
- version column?
- approval configuration table?
- approval request/decision table?
- requester/actor references?
No speculative audit table if History remains deferred.
No cascade unless documented.

17. REST actions
Evaluate explicit actions:
POST /api/v1/companies/{companyId}/financial-accounts/{id}/submit-for-approval
POST /api/v1/companies/{companyId}/financial-accounts/{id}/approve
POST /api/v1/companies/{companyId}/financial-accounts/{id}/reject
Reject body likely includes justification.
Do not add PATCH status.
Actor identity must not be trusted from arbitrary body unless temporary actor-context design explicitly approves it.

18. Errors
Propose only needed stable codes, e.g.:
FINANCIAL_ACCOUNT_INVALID_STATUS
SELF_APPROVAL_NOT_ALLOWED
APPROVER_NOT_ALLOWED
REJECTION_JUSTIFICATION_REQUIRED
APPROVAL_CONFLICT
Reuse FINANCIAL_ACCOUNT_NOT_FOUND/VALIDATION_ERROR/etc.
Avoid over-fragmentation.

19. History boundary
History persistence remains deferred.
Still determine what MUST be persisted now so requester/approver/rejection data is not lost.
Do not use "history deferred" to discard required business data.

20. Domain tests
Plan:
DRAFT -> PENDING_APPROVAL
DRAFT -> APPROVED if disabled semantics resolve that way
PENDING_APPROVAL -> APPROVED
PENDING_APPROVAL -> DRAFT on rejection
invalid source states
no generic mutation
rehydration/version if applicable.

21. Application tests
Plan:
submit required/disabled
approve valid
self-approval forbidden
invalid approver/permission
reject valid
blank justification
wrong status
cross-company
concurrency conflict
repeated actions according to approved semantics.

22. MVC tests
Cover routes, transitions, strictness, justification, stable errors, traceId,
invalid IDs, no arbitrary status field, no Idempotency-Key if deferred.

23. PostgreSQL tests
PostgreSQL 16 Testcontainers.
Depending on final design:
V9, Hibernate validate, optimistic-lock stale write, configuration persistence,
approval record persistence, no cascade, FK protection, cleanup isolation.

24. Auth deferral
Do not implement JWT/OIDC/Spring Security/login.
But do not weaken approval business semantics.
Clearly separate actor/permission abstraction from future auth mechanism.

25. Documentation
After decisions are approved, document state transitions, configuration,
requester/approver semantics, rejection justification, concurrency, auth boundary,
history boundary. Technical names in English.

26. Scope exclusions
No settlement, payment, receipt, reversal, FinancialMovement, full cancellation,
cash flow, dashboard, reports, external auth, notifications, Kafka, Redis, outbox.

27. Blocking decisions
Explicitly surface:
1. approval configuration model/granularity
2. actor identity model while auth deferred
3. where requester identity persists
4. how approvers are represented/validated
5. rejection justification persistence
6. separate ApprovalRequest/Decision entity?
7. behavior when approval disabled
8. repeated submit/approve/reject semantics
9. whether self-approval rule also applies to rejection
10. optimistic locking/version
11. idempotency

28. Exact files
List files grouped by Domain changes, Approval Application, persistence, REST,
V9, tests, shared errors/bootstrap, docs.

29. Output
Return:
1. doc findings/conflicts
2. blockers
3. transition graph
4. approval config design
5. actor/requester/approver design
6. rejection persistence
7. domain methods
8. use cases
9. concurrency
10. idempotency
11. V9
12. REST
13. errors
14. tests
15. files
16. Git workflow

Finish exactly:
PLAN STATUS: READY FOR APPROVAL
or
PLAN STATUS: BLOCKED

Do not modify files.
```
