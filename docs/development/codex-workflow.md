# Codex Development Workflow

## Official flow

```text
Documentation
   ↓
TECH / UC Issue
   ↓
Codex planning task
   ↓
Human review of plan
   ↓
Codex implementation task
   ↓
Automated validation
   ↓
Codex review task
   ↓
Human review
   ↓
Pull Request
```

## Persistent project instructions

Codex must read the root `AGENTS.md`.

`AGENTS.md` defines how Codex works.
The `docs/` tree defines what the system must do.

Do not copy all business rules into `AGENTS.md`.

## Planning

For non-trivial changes, ask Codex to:

1. read `AGENTS.md`;
2. read the task/Issue;
3. read referenced docs;
4. inspect current code;
5. identify files/dependencies;
6. identify tests;
7. identify risks/conflicts;
8. produce a plan without changing code.

## Implementation

Java formatting is enforced by Spotless. Apply and check it with:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
```

`spotless:check` is also bound to Maven `validate`, so `test`, `package`, and
`verify` reject unformatted Java.

After approving the plan, give Codex the task again with:

- approved decisions;
- strict scope boundary;
- required validation commands;
- explicit out-of-scope items.

Codex may edit files and run tests during this phase.

## Review

Run a separate review after implementation.

The review must compare the patch against:
- `AGENTS.md`;
- the TECH/UC;
- related ADRs;
- architecture;
- business rules;
- acceptance criteria.

The review should not change files unless explicitly requested.

## Missing decisions

If Codex finds an undocumented business decision, implementation must not silently choose one.

Use this loop:

```text
Codex identifies gap
  ↓
Decision is discussed
  ↓
Documentation is updated
  ↓
Codex continues
```
