# Codex Implementation Prompt Template

```text
Read AGENTS.md first.

Implement the approved plan for:

<TASK-ID> — <TASK NAME>

Use the approved plan as the scope boundary.

Required:
- follow the task acceptance criteria;
- follow all referenced documentation;
- keep architecture boundaries;
- implement only this task and direct dependencies;
- update tests required by the task;
- update documentation only when the task changes documented behavior/configuration.

Do not:
- implement future TECHs or UCs;
- add speculative abstractions;
- introduce undocumented business rules;
- implement authentication unless explicitly in scope.

Validation:
- run <commands>;
- inspect the diff;
- report any command that cannot run and why.

At the end report:
1. files created;
2. files modified;
3. tests/checks executed;
4. results;
5. deviations from the approved plan;
6. unresolved decisions.

Do not claim checks passed unless they were actually executed.
```
