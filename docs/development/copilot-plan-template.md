# Copilot Plan Prompt Template

Use este template substituindo `UC-XXX` e os arquivos específicos.

```text
You are planning the implementation of UC-XXX.

Before proposing changes, read:

.github/copilot-instructions.md

Then read the Use Case and all referenced:
- Business Rules
- Domain documentation
- ADRs
- Architecture documentation
- Database conventions
- API conventions

Inspect the existing code related to the feature.

Do not write code yet.

Create an implementation plan containing:

1. Business rules involved
2. Domain changes
3. Application components
4. Repository changes
5. Persistence changes
6. Flyway migration requirements
7. API changes
8. Authorization requirements
9. Transaction and concurrency considerations
10. Tests required
11. Files expected to be created or modified
12. Dependencies
13. Risks
14. Missing or conflicting requirements

Do not invent missing business rules.
If documentation is insufficient or conflicting, report the problem instead of choosing behavior.
```
