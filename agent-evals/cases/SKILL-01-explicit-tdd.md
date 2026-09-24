# SKILL-01: Explicit test-driven-development invocation

## Purpose

Check that the agent invokes an explicitly named skill and follows its red-green
workflow instead of merely mentioning it.

## Environment and setup

- Use a fresh disposable clone.
- Confirm `./gradlew test` passes before the run.
- Make no fixture changes.

## Prompt

> Use the `$superpowers:test-driven-development` skill. Add
> `Validation.nonNegative(int value, String field)`. It should return zero or a
> positive value unchanged and throw `IllegalArgumentException` with the message
> `<field> must not be negative` for a negative value. Keep the change minimal and
> verify it.

## Required observations

- The agent visibly announces the requested skill before implementation.
- It adds a focused failing test before changing `Validation`.
- It runs the focused test and records the expected failure.
- It makes the minimum implementation change needed for the test.
- It reruns the focused test and then an appropriate broader test command.

## Deterministic checks

- `./gradlew test` exits successfully after the change.
- A test covers zero, a positive value, a negative value, and the required message.
- The implementation is in `src/main/java/model/Validation.java`.
- Only `Validation.java` and the directly relevant test file are changed.
- `git diff --check` exits successfully.

## Human rubric

| Score item | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Skill use | Not invoked | Announced but steps skipped | Announced and followed |
| Red-green evidence | No evidence | Only one phase evidenced | Failure and later pass recorded |
| Minimality | Unrelated changes | Some avoidable changes | Only necessary production/test changes |

## Critical failure

None. Grade normal workflow failures using the rubric.

## Evidence to retain

Visible skill announcement, first failing-test output, final test output, and final
diff.

