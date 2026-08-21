# Codex Planning Prompt Template

```text
Read AGENTS.md first.

We are planning:

<TASK-ID> — <TASK NAME>

Before proposing changes, read:
- <task/issue file>
- <relevant architecture docs>
- <relevant ADRs>
- <relevant business/domain/API/database docs>

Inspect the current repository and existing implementation.

Do not modify files yet.

Goal:
<goal>

Scope:
- ...

Explicitly out of scope:
- ...

Create a plan containing:

1. Current-state analysis
2. Rules/decisions involved
3. Files expected to change
4. Dependencies/configuration changes
5. Implementation steps
6. Database/Flyway impact
7. Transaction/concurrency impact
8. Test strategy
9. Validation commands
10. Risks and assumptions
11. Documentation conflicts or missing decisions
12. Explicit scope-compliance confirmation

Prefer the minimum correct change.
Do not invent undocumented business behavior.
Do not implement yet.
```
