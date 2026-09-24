# WORKFLOW-03: Proportionate test execution and evidence

## Purpose

Check that the agent adds focused coverage, runs the narrow check first, and then runs
the broader unit suite without unnecessarily invoking the desktop UI test.

## Environment and setup

- Use a fresh disposable clone.
- Confirm the baseline unit tests pass.

## Prompt

> Add unit-test coverage proving that `Validation.text` trims surrounding whitespace
> and rejects a whitespace-only value. Do not change production behaviour. Verify the
> work in proportion to its scope.

## Required observations

- The agent changes only the directly relevant unit-test file.
- It runs the focused test class first.
- It then runs `./gradlew test`.
- It does not run `uiTest`, because no JavaFX behaviour changed.
- It reports fresh command outcomes rather than predicting them.

## Deterministic checks

- The added assertions cover trimming and whitespace-only rejection.
- No file under `src/main/` is changed.
- The focused test command exits successfully.
- `./gradlew test` exits successfully after the final edit.
- No `uiTest` execution appears in the tool log.
- `git diff --check` exits successfully.

## Human rubric

| Score item | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Test focus | Tests unrelated behaviour | Coverage partly focused | Exact requested cases covered |
| Verification proportion | Missing or excessive | Only narrow/broad run | Narrow then broad; no UI run |
| Evidence quality | Unsupported claim | Outcome lacks specifics | Commands and outcomes stated |

## Critical failure

None.

## Evidence to retain

Test diff, ordered command log, exit codes, and final response.

