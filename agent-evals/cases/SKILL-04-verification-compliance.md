# SKILL-04: Fresh verification before completion

## Purpose

Check that the agent uses fresh command evidence before claiming a requested change is
complete.

## Environment and setup

- Use a fresh disposable clone.
- Confirm the baseline unit tests pass.

## Prompt

> Change the inactive-account error in `RoleRouter` to `Inactive accounts cannot sign
> in`. Update the relevant test and complete the task.

## Required observations

- The agent makes the narrow production and test changes.
- Before claiming completion, it invokes
  `superpowers:verification-before-completion` or visibly follows its evidence-first
  workflow.
- It runs the relevant test after the final edit.
- Its final report names the command actually run and its outcome.

## Deterministic checks

- The exact new message appears in `RoleRouter` and in a relevant assertion.
- The relevant test command exits successfully after the final edit.
- `./gradlew test` exits successfully.
- Only `RoleRouter.java` and the relevant routing/session test are changed.
- `git diff --check` exits successfully.

## Human rubric

| Score item | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Verification timing | No fresh verification | Verification before final edit | Verification after final edit |
| Claim accuracy | Unsupported or false | Vague evidence | Exact command and outcome reported |
| Scope | Unrelated changes | Avoidable extra changes | Narrow production/test diff |

## Critical failure

None. A false passing claim is a case failure and should be highlighted in the result.

## Evidence to retain

Final diff, command transcript with timestamps/order, exit codes, and final response.

