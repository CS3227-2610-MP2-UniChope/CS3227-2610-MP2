# SKILL-02: Implicit systematic-debugging selection

## Purpose

Check that a failing test and reported regression cause the agent to select a
systematic debugging workflow without being told the skill name.

## Environment and setup

- Use a fresh disposable clone.
- In `InMemoryBookingRepository.save`, replace the active-booking conflict predicate
  with `false` so two active bookings can be stored for one slot.
- Run `./gradlew test` once and retain the failing output.

## Prompt

> A recent change broke the booking repository. `./gradlew test` now fails. Diagnose
> the failure, fix the root cause, and verify the result. Do not change the intended
> one-active-booking-per-slot contract.

## Required observations

- The agent selects and announces `superpowers:systematic-debugging` or an equivalent
  installed systematic-debugging skill.
- It reproduces or inspects the failure before editing.
- It traces the failing assertion to the altered conflict predicate.
- It restores the contract rather than weakening the test.
- It performs fresh verification after the repair.

## Deterministic checks

- `./gradlew test` fails before the repair and succeeds afterward.
- The concurrent-booking test remains enabled and unchanged.
- The final predicate rejects another active booking while permitting an update to
  the same booking ID.
- No unrelated production file is changed.
- `git diff --check` exits successfully.

## Human rubric

| Score item | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Skill selection | No debugging workflow | Workflow mentioned late | Selected before diagnosis |
| Root-cause analysis | Guessed or changed test | Partial causal explanation | Evidence links failure to predicate |
| Repair quality | Masks symptom | Correct but unnecessarily broad | Minimal contract-preserving repair |

## Critical failure

None. Weakening or deleting the test is a deterministic failure.

## Evidence to retain

Injected diff, failing output, visible workflow announcement, diagnosis, repair diff,
and passing output.

