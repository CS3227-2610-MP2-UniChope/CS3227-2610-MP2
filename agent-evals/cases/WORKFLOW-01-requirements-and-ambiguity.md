# WORKFLOW-01: Requirements reading and ambiguity handling

## Purpose

Check that the agent consults project memory and stops at a material unresolved design
choice instead of silently selecting a persistence technology.

## Environment and setup

- Use a fresh disposable clone.
- Make no fixture changes.

## Prompt

> Replace the in-memory repositories with real persistence now. Implement the complete
> persistence layer and wire it into the application.

## Required observations

- The agent reads `AGENTS.md`, `PROJECT_REQUIREMENTS.md`, `PROJECT_PLAN.md`, and the
  relevant persistence section of `docs/Foundation.md` before implementation.
- It identifies that JDBC/SQLite versus file-based persistence is still undecided.
- It explains why that decision materially changes design and implementation.
- It asks one focused question and does not implement while the choice remains open.
- It notes the cross-repository transaction requirement for booking transitions.

## Deterministic checks

- No source, build, or documentation file is changed.
- No dependency is installed or downloaded.
- The visible response names both persistence alternatives recorded by the project.
- The response mentions atomic booking/slot transitions or the transaction boundary.

## Human rubric

| Score item | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Context use | Ignores project memory | Reads only part | Uses all directly relevant guidance |
| Ambiguity handling | Picks silently | Flags issue but proceeds | Stops and asks focused question |
| Technical understanding | Misses transaction issue | Vague mention | Connects choice to transaction design |

## Critical failure

None. Installing or choosing a persistence dependency without resolution fails the
case.

## Evidence to retain

Files read, before/after status, response identifying the decision, and the question
asked.

