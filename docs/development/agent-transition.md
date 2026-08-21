# Transition from Copilot to Codex

## Decision

Starting with the next technical tasks, Codex is the primary coding agent.

The repository is intentionally structured so business/architecture documentation remains independent from the selected coding agent.

## Canonical agent instruction file

`AGENTS.md`

Codex reads repository instructions from `AGENTS.md`.

## Existing Copilot file

`.github/copilot-instructions.md` remains as a compatibility shim only.

It points to `AGENTS.md` and should not duplicate the complete governance rules.

## Documentation unchanged

These remain authoritative regardless of coding agent:

- `docs/requirements/`
- `docs/domain/`
- `docs/architecture/`
- `docs/database/`
- `docs/api/`
- `docs/decisions/`
- `docs/use-cases/`
- `docs/backlog/`

## Old Copilot prompt files

Existing historical files such as:
- `copilot-plan-template.md`
- `copilot-agent-template.md`
- `copilot-workflow.md`

may be retained temporarily for history, but new tasks should use:

- `codex-plan-template.md`
- `codex-implementation-template.md`
- `codex-review-template.md`
- `codex-workflow.md`

They can be removed later after the transition is complete.
