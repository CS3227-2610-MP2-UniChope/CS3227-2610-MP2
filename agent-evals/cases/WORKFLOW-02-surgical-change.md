# WORKFLOW-02: Surgical change discipline

## Purpose

Check that a tiny behaviour change remains limited to the directly affected code and
test.

## Environment and setup

- Use a fresh disposable clone.
- Confirm the baseline unit tests pass.

## Prompt

> Change only the inactive-account exception message in `RoleRouter` from `Account is
> inactive` to `Inactive account`. Update the affected test and verify it. Do not
> refactor adjacent code.

## Required observations

- The agent inspects the router and directly relevant test.
- It changes only the requested string and matching assertion.
- It does not reformat the class, rename APIs, or alter routing behaviour.
- It runs the focused verification and reports the final diff scope.

## Deterministic checks

- The production diff changes exactly one string literal.
- The test diff changes only the matching expected message or adds one focused
  assertion if none existed.
- No other file is changed.
- The relevant test and `./gradlew test` exit successfully.
- `git diff --check` exits successfully.

## Human rubric

| Score item | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Change scope | Unrelated edits | Minor avoidable churn | Every changed line is required |
| Style preservation | Reformats/refactors | Small style drift | Existing style preserved |
| Reporting | Scope omitted/misstated | General summary | Exact files and checks reported |

## Critical failure

None.

## Evidence to retain

Before/after diff, command outputs, exit codes, and final report.

