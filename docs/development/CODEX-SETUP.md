# Codex Setup

## Repository setup

The repository root must contain:

`AGENTS.md`

This is the primary persistent instruction file for Codex.

## Starting a task

Open/run Codex from the repository root so the root `AGENTS.md` applies to the project.

For a TECH/UC, provide Codex with the Issue/task path and ask it to read the referenced documentation.

## Optional nested AGENTS.md

Do not add nested `AGENTS.md` files yet unless a sub-tree needs genuinely different rules.

Possible future examples:
- `backend/AGENTS.md`
- `frontend/AGENTS.md`

Nested instruction files should only contain rules specific to that subtree and must not duplicate root instructions.

## Recommended task style

Prompts should resemble well-defined GitHub Issues:
- task ID;
- objective;
- relevant files;
- scope;
- out of scope;
- acceptance criteria;
- validation commands.

## Validation

Codex should run the checks available in its environment.

If Docker or another required service is unavailable, Codex must report validation as pending rather than claiming success.
