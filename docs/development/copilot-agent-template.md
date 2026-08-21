# Copilot Agent Prompt Template

```text
Implement the approved plan for UC-XXX.

Follow:
.github/copilot-instructions.md

Use the approved implementation plan as the scope boundary.

Requirements:
- implement only UC-XXX and direct dependencies;
- follow documented business rules;
- preserve architecture boundaries;
- do not implement future Use Cases;
- create/update required tests;
- use PostgreSQL conventions;
- use camelCase database identifiers;
- use Flyway for schema changes;
- do not use H2;
- do not implement authentication unless the authentication architecture is documented;
- do not introduce undocumented business rules.

After implementation:
1. compile the project;
2. run relevant unit tests;
3. run relevant integration tests;
4. verify Flyway migrations;
5. report created/modified files;
6. report deviations from the approved plan;
7. report unresolved decisions.
```
