# Huanghao — Reflection

### 1. Starter Prompt to initialise the development of the features
My original prompt established my role, the project context, and the boundaries for the work:
```
I am Person B and own the Tutor role for UniChope.
Read AGENTS.md, PROJECT_REQUIREMENTS.md, PROJECT_PLAN.md, and docs/Foundation.md before planning or editing.
Current state:
- Shared foundation is merged into main.
- I own tutor UI/logic, tutor tests, and SQLite/JDBC persistence.
- Keep changes mainly in tutor/, data/sqlite/, and matching test packages.
- Do not modify other teammates’ untracked files.
- Before implementing, identify any shared-contract changes required and explain them first.
  My first task: plan and implement TutorService tests before the tutor UI.
```
After I sent this prompt, the agent explained that we needed to decide the TutorService responsibilities and API before writing tests. It gave me a list of decisions to make, including how tutors could cancel slots, when they could mark bookings complete, what inactive tutors could still view, and whether completing a booking required a shared atomic operation. I answered these questions before implementation began, so the service behavior could reflect my requirements.

The agent also recorded the decisions and implementation sequence in the Person B development log. That gave it a reference for the agreed service behavior and planned tests as the work continued. I thought this was useful because earlier decisions could be checked later. The plan was kept in the development log, while AGENTS.md remained the source of project instructions.

For example, I asked:
```
“what about making delete unavailable, rather a rescheduling is needed?”
```

I then clarified:
```
“keep reschedule to later, so the delete will be rejected and require student to cancel first”
```

The agent then treated rescheduling as out of scope and rejected tutor cancellation of a booked slot until the student cancels the booking. I think this was handled well: the agent surfaced an unclear rule and waited for my decision instead of choosing a behavior for me.

Overall, I learned that an agent may question or refine a request rather than implement it immediately. It can explain its interpretation and ask me to choose between behaviors, but I still need to check that its proposal matches what I want. In this case, the decision prompts helped me clarify the requirements before implementation. I would evaluate the work by comparing the chat decisions with the resulting service code, tests, and test output in the detailed development log.


### 2. Self-assessment of agent workflow

To check that the agent understood the task and followed the intended process, I asked it to use test-driven development for each behavior. I wanted it to verify the baseline, demonstrate that each new test detected missing behavior, and avoid claiming a red-green cycle without observing both results.

I send this prompt:

```
Use test-driven development for each behavior. Run the relevant baseline tests first. Then add one focused test and run it before changing production code. Confirm that it fails for the expected reason. If it passes immediately or fails for an unrelated reason, stop and fix the test or plan before implementing.
```

The detailed log records one example of this process. The baseline ./gradlew test passed. Then InMemoryConsultationLifecycleTest failed because Repositories.lifecycle() was missing. After the lifecycle contract and implementation were added, the focused test passed. Tests for invalid booking and slot states also failed before validation was implemented, then passed afterward.

I consider this useful evidence that the agent checked its workflow as well as the implementation. I would still compare the log with the actual test output before relying on the recorded results.


### 3. Keeping context across a long interaction

At the start, the development log recorded decisions, code changes, commands, and test
results in detail. Later entries were shorter and focused more on outcomes. This made it
harder for me to follow how some decisions led to implementation and how the changes were
verified. I cannot tell from the log alone whether the agent lost context or simply wrote
less down, but the shorter entries left me with less evidence to review.

To reduce this problem, I would ask the agent to update a persistent work log after each
task. Each entry should record the request, decisions made, files changed, commands run,
observed results, and any unresolved issues. Before starting the next task, the agent
should review that log and carry forward the relevant decisions. I would also compare the
log with the code changes and test output, so the record stays useful for both the agent
and my evaluation of its work.

### Clear logs

```
### Context and constraints

- Replace only Person B's `TutorMainView` placeholder and add tutor-owned JavaFX
  presentation code under `tutor/`.
- Match the existing Admin workspace's Java-built JavaFX conventions: a padded
  `BorderPane`, heading/Refresh/Sign out top bar, non-closable `TabPane`, tables,
  control IDs, and one bottom message label for recoverable errors.
- Use normal JavaFX code, not FXML. The project has no FXML/controller convention
  and enables only `javafx.controls`; this avoids an unnecessary shared Gradle
  change.
- All business operations go through `TutorService`. The workspace must not call
  repositories directly.

### Components and data flow

`TutorMainView` will construct a session-bound `TutorService` with the existing
application `OperationLog` and pass it to a package-private `TutorWorkspace`.
The workspace refresh action re-queries service methods and repopulates controls;
the presentation layer catches expected `IllegalArgumentException` and
`SecurityException` values and displays their safe message in the bottom label.

The workspace has three non-closable tabs:

1. **Slots** — date selector, active-assigned module selector, start/end `HH:mm`
   inputs, Create and Cancel selected. Its table uses `findUpcomingSlots(date)`,
   so it contains only AVAILABLE and BOOKED slots.
2. **Bookings** — optional module, date, and booking-status controls; a
   `TutorBookingView` table from `findBookingViews(filter)`; and Complete selected.
3. **History & notes** — date/status selector for CANCELLED or COMPLETED slot
   history; a completed-booking view for selecting a consultation; and a note text
   area with Load/Save controls using `findNote` and `saveNote`.

The UI formats and interprets slot times in `Asia/Singapore`; the service remains
the authority for all activity, assignment, status, ownership, completion, and
note rules. A refresh after every successful mutation keeps the controls and
tables consistent.

### Testing and scope

- Preserve existing TutorService tests.
- Add focused unit tests for the Java-only parsing/formatting helpers where
  practical and a JavaFX `uiTest` smoke test that instantiates the workspace and
  verifies its tabs/essential controls are present.
- Do not implement rescheduling, direct repository access, FXML, SQLite/JDBC, or
  changes to other roles in this slice.
```
### High-level only logs

```
### Delivered behavior

- `findSlotHistory(date, status)` returns the signed-in tutor's CANCELLED or
  COMPLETED slots for a Singapore-local date, sorted by slot start time.
- AVAILABLE and BOOKED are rejected because they belong to the normal upcoming
  slot view.
- An inactive tutor may view only COMPLETED slot history, preserving the
  approved completed-consultation history rule.

### Test-first evidence

- The cancelled-history test initially failed at compilation because the new
  service method was absent. After the method was added, it passed.
- The AVAILABLE-status rejection test failed with an unmet `assertThrows`
  expectation before the history-status validation was added.
- The inactive completed-history test failed with `SecurityException` before
  the completed-only inactive-history allowance was added.
- The completed-status test is regression coverage for the status parameter's
  direct generalization, not a separate red-green cycle.

### Verification

- `./gradlew test --tests tutor.TutorServiceTest` passed.
- `./gradlew test` passed.
- `git diff --check` passed.

```

To reduce this problem, I ask the agent to update a persistent work log after each task. Each entry should record the request, decisions made, files changed, commands run, observed results, and any unresolved issues. Before starting the next task, the agent should review that log and carry forward the relevant decisions. I would also compare the
log with the code changes and test output, so the record stays useful for both the agent and my evaluation of its work.



# Tieu Trong Phuc's Reflection

Agentic AI was particularly useful for diagnosing a dependency-related bug. When a teammate forgot to update the database schema, I initially thought the application could not download a new dependency and struggled to identify the actual cause. The agent helped trace the failure to the outdated schema. Using the agent earlier would have saved me considerable debugging time.

If I were to redesign the UI, I would choose a consistent visual style before implementation. The references in `design.md`, from [Refero](https://styles.refero.design/), became helpful only after I had already spent hours experimenting, and the resulting UI still lacked consistency. This experience taught me that even a capable agent needs explicit instructions and checkpoints.

Learned from MP1, I matched the agent's skills to each task. For service-layer logic, I instructed the agent to create unit tests alongside the implementation. I knew this approach was working when the agent automatically produced well written test cases that clearly verified the behaviour of the newly implemented services.
