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



# Tieu Trong Phuc Reflection

## 1. Agents are excel at diagnosing problems outside the code I am looking at

While working on the project, I ran into a dependency-related bug. A teammate had forgotten to update the database schema, but I assumed the application was failing to download a new dependency. I spent a long time investigating the wrong cause. When I finally asked the agent to investigate, it traced the failure back to the outdated schema.

This showed me that an agent can catch a wrong assumption because it inspects the actual evidence rather than the theory I have already committed to. I should have brought it in as soon as my first debugging attempts stalled, since doing so earlier would have saved considerable time.

## 2. Design decisions need to be made before implementation, not during it

If I were to redesign the UI, I would choose a consistent visual style before writing any code. The references in `design.md`, taken from [Refero](https://styles.refero.design/), only became useful after I had already spent hours experimenting, and the resulting UI still lacked consistency. Because the agent had no shared style to work from in the early stages, each change it made reflected the prompt in front of it rather than an overall design.

This taught me that even a capable agent needs explicit instructions and checkpoints. In future projects, I should settle the design direction first, put it in a reference file, and review the UI at set checkpoints instead of correcting inconsistencies afterward.

## 3. Matching the agent's skills to each task improves the results

Building on what I learned from MP1, I matched the agent's skills to each task. For service-layer logic, I instructed the agent to write unit tests alongside the implementation. I knew this approach was working when the agent automatically produced well-written test cases that clearly verified the behavior of the newly implemented services.

This confirmed that the agent performs better when I tell it how to work, not only what to build. Asking for tests as part of the task also gave me a quick way to verify the generated code.

## 4. Recurring problems should become part of the agent's workflow

During MP1, I did not encounter any unused imports, but they began to appear in the code generated for this project. Rather than removing them by hand each time, I added a final step to the agent's skill that rechecks the imports once the work is complete. In later iterations, the same problem did not reappear.

This showed me that when a problem repeats, the better fix is to update the agent's instructions rather than clean up after it each time.

## Overall

- Agents are especially helpful for debugging, and I should use them earlier instead of struggling with my own assumptions.
- Design direction, such as a consistent UI style, should be decided before implementation, with checkpoints along the way.
- Matching the agent's skills to the task, for example by requiring unit tests with service logic, produces more reliable and easier-to-verify results.
- Recurring issues should be fixed by improving the agent's skills, not by repeated manual cleanup.

# Kok Seng Reflection

*Draft prepared with Codex assistance from the recorded interactions in
[my contribution log](../logs/kokseng.md). Pending my verification before
submission. The examples below describe instruction-guided agent skills used
during development; they do not claim that three packaged SKILL.md plugins were
created or evaluated.*

My responsibilities were the Admin role, the testing framework, and a basic
logging/monitoring utility shared by all three roles. I used a single Codex agent
with project instructions, explicit business decisions, and test-based checks.
The shared-foundation work preceded the role implementations; the later SQLite,
authentication and interface changes also include teammates' contributions.

## 1. Turning ambiguous requirements into testable rules

The first useful skill was requirements clarification before implementation.
The instruction was to read the assignment and current project plan, identify
ambiguities, and keep changes within the agreed role responsibilities. A request
such as “remove a user” was not enough to decide whether historical bookings
should disappear or whether an unresolved booking should block the action.

On 25 September I confirmed three rules: deactivate instead of permanently
delete; block affected ACTIVE bookings, including overdue ones, or future
AVAILABLE slots; and calculate cancellation/completion rates over all bookings,
with 0% for empty data. These decisions gave the agent concrete acceptance
criteria instead of leaving product behavior to inference.

Verification was tied to those criteria. AdminServiceTest checks that overdue
active bookings block deactivation and unassignment, that unrelated records do
not block an action, and that history survives deactivation. AdminQueriesTest
checks mixed statuses, empty data, rebookings and same-named tutors. The names
and assertions of these tests make the decisions reviewable.

The lesson from this example is that a clarification is useful only when it
changes a checkable requirement. In another project I would record each agreed
rule beside its corresponding test so later changes cannot silently replace it.

## 2. Preserving shared contracts while integrating role work

The second skill was inspecting the current repository before extending it.
Project instructions required separate role packages and reuse of the shared
repository bundle. That mattered because an admin action can affect another
role: a tutor might publish a slot while an admin is trying to deactivate them.
Checking for blockers and then writing through unrelated operations would leave
a gap between the check and the change.

The admin implementation introduced a narrow withExclusiveAccess boundary.
Initially the fake implementation used its shared lock; SQLite transaction
semantics were integration work for Person B. The admin tests exercised the
guarded protocol and rejected unsupported storage rather than reporting a
successful mutation. This was not evidence of a finished database integration
at that stage.

On 29 September, reviewing the merged code changed the picture: SQLite,
authentication and the role workflows were implemented. The responsibility audit
then found that the Admin form could provision Tutor/Admin accounts but not
Students. An identity-only addUser method and student self-registration did not
complete the admin-facing requirement. The fix reused the existing authenticated
provisioning flow, including credential storage and mandatory password change,
rather than creating a second account mechanism.

This showed why the agent must compare requirements with the actual UI and
current contracts, not merely find a similarly named method. Tests verified
student login after reopening SQLite, duplicate-email rejection, password-change
requirements and denied access for unauthorized actors.

## 3. Verifying behavior at service, UI and diagnostics boundaries

The third skill was choosing checks that matched the failure being investigated.
The agent was asked to test implementation work and keep concrete results in the
log. Pure query tests were appropriate for statistics; service tests covered
authorization and business rules; the JavaFX workflow checked that those actions
were reachable through the Admin controls.

The shared OperationLog required another kind of check. A logging failure must
not turn a successful database write into an apparent failure. OperationLogTest
and the admin failure-handling tests verify that behavior and the diagnostic
counters. The later RoleOperationLogTest calls real admin, tutor and student
services against one SQLite bundle, checks their shared success/failure totals,
and verifies actor attribution without personal input or exception messages.

The final implementation check on 29 September passed 152 non-UI tests, 8
desktop tests and installDist on Windows. Those are project-wide totals, not
tests entirely authored for my role. The generated Admin screenshot was also
inspected for readable controls. These results support the tested workflows;
they do not prove macOS/Linux compatibility or provide external monitoring.

I would retain this layered approach, but keep the claim beside its evidence:
a unit test, a desktop interaction and a build each establish different things.
The recorded admin work does not demonstrate a test-first red/green cycle, so I
would not describe it as having followed one.

## Guidance, corrections and future improvements

The agent needed correction on scope and record keeping. It initially created
a separate admin roadmap document; I asked for it to be removed. I also asked
for all my interaction summaries to be consolidated into logs/kokseng.md and
for local planning/memory files to remain out of the repository. These were
concrete workflow changes rather than new application features.

Persistent notes also needed maintenance. After teammates merged substantial
changes, descriptions of demo login and in-memory storage were obsolete. During
the sync, checking out an older local branch removed formerly tracked memory
files, which the agent then restored. That recovery was avoidable extra work.
A future sync instruction should require checking historical tracking and
preserving local-only files before changing branches.

The latest documentation instruction adds another boundary: update my own
sections without rewriting teammates' work. I would include this ownership rule
from the start, along with a final comparison of edited files and sections.
Generated reflections and interaction summaries still need my review; the
agent's account should not be accepted as my personal experience without checking.

For this project, the most useful customization was a repeatable process:
read current requirements and contracts, ask about material ambiguities, implement
within ownership boundaries, run relevant checks, and record observed results
and remaining limitations. More automation would help only if that evidence
remains easy to inspect.
