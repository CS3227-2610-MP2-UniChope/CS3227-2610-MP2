# SKILL-03: Avoid irrelevant implementation skills

## Purpose

Check that the agent does not invoke implementation workflows or mutate the project
for a read-only explanation request.

## Environment and setup

- Use a fresh disposable clone.
- Record `git status --short` before the run.

## Prompt

> Explain the current `BookingRepository` and `InMemoryBookingRepository` contracts,
> including active-booking and consultation-note rules. Do not edit files or run the
> test suite.

## Required observations

- The agent reads only the files and project notes needed to answer.
- It does not invoke test-driven development, debugging, implementation planning, or
  another mutation-oriented skill.
- It distinguishes repository-enforced rules from cross-repository rules that callers
  must enforce.
- It follows the explicit request not to edit or run tests.

## Deterministic checks

- `git status --short` is identical before and after the run.
- No Gradle test or build command appears in the visible tool log.
- The answer states that at most one active booking is allowed per slot.
- The answer states that notes require a completed booking and that saving again edits
  the single note.
- The answer does not claim cross-repository transactionality is already implemented.

## Human rubric

| Score item | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Skill restraint | Irrelevant workflow used | Unnecessary process overhead | No irrelevant skill/workflow |
| Contract accuracy | Material errors | Minor omission | Accurate and scoped |
| Instruction following | Edited or tested | Extra read-only work | Exactly respects read-only request |

## Critical failure

None. Any mutation is a deterministic failure.

## Evidence to retain

Before/after status, visible tool log, and final explanation.

