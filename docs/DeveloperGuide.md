# UniChope developer guide

## Current architecture

Java SE 25 and JavaFX provide the desktop app. Immutable records in model represent
users, modules, slots, bookings, and notes. Repository interfaces separate features
from storage. The application injects one SQLite-backed `Repositories` bundle into
all role views. It opens `~/.unichope/unichope.db`, creates schema version 1 when
needed, and seeds demo accounts only for an empty database. The bundle includes the
consultation lifecycle. Student remains incomplete; this guide's feature detail
otherwise focuses on Admin design.

## Admin design

AdminMainView keeps the existing RoleView signature. AdminWorkspace owns JavaFX
controls and delegates to AdminService. AdminService re-reads the actor ID on every
operation, requires an active Admin, validates business rules, and uses injected
Clock and OperationLog dependencies. It makes at most one repository write after
validation for each admin action. AdminSnapshot provides one immutable view of the
repository state; AdminQueries performs pure joins and statistics over that snapshot.

No existing model, Repositories constructor, Student/Tutor screen, or routing API
was changed for these admin features. Admin controllers use the repository interfaces,
not concrete fake classes. The UI currently calls services synchronously; reassess
this when integrating slower persistence to keep the JavaFX thread responsive.

## Shared storage coordination

`ConsultationLifecycle.withExclusiveAccess(Supplier<T>)` is an additive shared API.
Both the in-memory fake and SQLite bundle coordinate all repositories in their own
bundle. SQLite opens short-lived JDBC connections and runs guarded operations in one
transaction; a failure rolls back its writes. `completeActiveBooking` changes an
ACTIVE booking and BOOKED slot to COMPLETED in that same transaction.

The interface default throws `UnsupportedOperationException`, preserving source
compatibility for any storage implementation that does not provide guarded updates.
Do not build a production bundle from independently locked repositories or unrelated
database connections.

Person A/B booking and slot-creation services must re-check active users/modules and
tutor-module assignments within the same atomic boundary. A repository save by itself
does not validate cross-entity business rules. SQLite regression tests cover reopen
persistence, duplicate validation, transaction rollback, and booking completion;
they do not claim multi-process database concurrency.

Deactivation preserves identity and history, and no bulk cancellations are performed.
ACTIVE bookings block even if overdue. Future AVAILABLE means startTime is strictly
after the injected clock's current instant. Unassignment checks only the selected
pair. Module edits preserve ID and status. Assignments to inactive entities are
rejected. See UserGuide for user-facing actions and statistics definitions.

## Shared logging and basic monitoring

OperationLog.application() uses the JDK logger `unichope.operations` and existing
handlers (console by default). It adds no global handlers, dependencies, or changes
to repository signatures. Other roles can adopt it independently:

```java
OperationLog.application().record("tutor.slot.create", actorId, slotId, null);
// On failure: pass the RuntimeException as the last argument.
```

Use fixed operation identifiers, never user input. Events include timestamp,
operation, actor/entity UUID, outcome, and exception type. They deliberately omit
exception messages, emails, names, credentials, and consultation notes. Thread-safe
counters expose successes, failures, and log delivery failures through metrics().
Logging failures do not change the result of a successful operation. No persistent
monitoring dashboard or rotated log files are claimed: configure JDK logging handlers
for durable diagnostics as needed. Automated tests inject an event sink and clock.

## Testing and team boundaries

JUnit is retained; there are no dependency upgrades. Admin and util tests live in
separate packages. `test` excludes UI tests, and `uiTest` forks per test class because
JavaFX cannot restart after Platform.exit. Existing role-routing and lifecycle tests
are preserved. The desktop admin test creates its own fixtures and saves screenshots
under build/reports/admin-snapshots for inspection.

Person C owns admin tests and shared logging; Person A retains CI/CD and Person B
retains persistence. Run focused tests during changes, full unit tests before a PR,
and desktop tests for UI changes. Test real storage and supported operating systems
before release. No production release has been validated by these development tests.

## Acknowledgements

The existing project foundation and consultation lifecycle are team contributions.
This admin implementation and its tests were developed with Codex assistance;
logs/kokseng.md records interactions pending human verification. JavaFX, Gradle,
JUnit, and the JDK logging facilities are external dependencies/tooling; the Gradle
wrapper is generated tooling. No external application code was copied for this work.
