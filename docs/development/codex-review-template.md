# Codex Review Prompt Template

```text
Read AGENTS.md first.

Review the implementation of:

<TASK-ID> — <TASK NAME>

Read:
- <task/issue>
- <relevant documentation>

Inspect all files changed by the task.

Do not modify files.

Review for:
1. scope compliance;
2. undocumented behavior;
3. architecture violations;
4. dependency creep;
5. database/Flyway violations;
6. multi-company isolation;
7. transaction/concurrency problems;
8. test gaps;
9. configuration errors;
10. documentation inconsistencies.

Classify findings:
- CRITICAL
- HIGH
- MEDIUM
- LOW

For each finding include:
- file/path;
- issue;
- why it matters;
- recommended correction.

If a category has no findings, say it passed.

End with exactly one:

REVIEW RESULT: APPROVED

or

REVIEW RESULT: CHANGES REQUIRED

Do not make code changes during the review.
```
