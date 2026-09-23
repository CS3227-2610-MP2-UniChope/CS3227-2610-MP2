# Shared foundation

## Implemented scope

- Java 25 immutable domain values: sealed User abstraction with Student, Tutor, and
  Admin records; Role; Module; TutorModule; ConsultationSlot; Booking;
  ConsultationNote; BookingStatus and SlotStatus.
- Four repository interfaces and independent in-memory implementations. A
  Repositories bundle is injected into each role view through the shell.
- JavaFX demo account selector, active-user session handling, explicit routing for
  all three roles, role-owned placeholder screens, and sign-out.
- Gradle wrapper, JavaFX dependencies, JUnit foundation tests, and a separate
  JavaFX UI smoke test. No CI/CD, durable persistence, or business workflows yet.

## Names and ownership

Packages match merged PR #1, without `com.app`: `model.user`, `model.module`,
`model.consultation`, `data.repository`, `data.memory`, `student`, `tutor`, `admin`,
`shell`, and `util`. `data.sqlite` remains an empty reserved directory. Shared
model validation is in `model.Validation`, not in the UI or persistence layer.

Role teams replace StudentMainView, TutorMainView, or AdminMainView while retaining
RoleView.create(User, Repositories, Runnable). Use the injected repositories; do
not create a fresh bundle per view or login. The Runnable signs out and returns to
login. The placeholders check their user's role but are not service authorization.
Future operations must authorize against the current session before accessing data.

Explicitly import `model.module.Module` because Java also has `java.lang.Module`.
Records use accessors such as `id()`, `name()`, and `isActive()`, not bean getters.

## Repository contract

`save` upserts an immutable value by UUID, `findById` returns Optional, and `findAll`
returns an unmodifiable snapshot. In-memory reads preserve insertion order; business
screens should sort explicitly. Missing IDs are not errors. IDs and entities cannot
be null. Inactive users/modules remain stored to preserve references and history.
No hard-delete API is exposed; deleting a slot is intended to be a cancellation
pending final business rules. The repository layer does not enforce who may call it.

Additional contract requirements (also required of future real implementations):

- User emails are unique ignoring case, including inactive users. Lookup trims input.
  A stored user's role cannot change under the same ID.
- Module codes are unique ignoring case. TutorModule pairs are unique; assign is
  idempotent and requires an existing module. Unassign returns whether a pair existed.
  ModuleRepository owns these operations, avoiding an unplanned fifth repository.
- Slot queries support tutor and module IDs; booking queries support student and slot
  IDs. Further filtering can initially compose these snapshots in role services.
- At most one ACTIVE booking per slot is accepted by BookingRepository.save. This
  check and save are synchronized in the fake. Cancelled booking history is retained,
  so another booking may use the same slot ID.
- BookingRepository owns one note per booking. Saving a note requires an existing
  COMPLETED booking; saving again edits it. A booking with a note cannot be changed
  to a non-completed status. Note ownership derives from the booking's slot/tutor.

The fakes intentionally do not validate references across separate repositories.
For example, the caller must check that an assignment's tutor exists, is a Tutor,
and is active. Likewise slot creation must validate tutor-module assignment and
active modules, and booking must validate student identity and slot availability.

**Do not implement booking by treating separate slot.save and booking.save calls as
an atomic transaction.** Per-repository synchronization only prevents duplicate
active booking records within one fake instance. A shared transaction/application
service contract is still needed for atomic slot and booking updates, cancellation,
and completion; the real persistence implementation must enforce this too. Model
withStatus methods are value-copy helpers, not validated business transitions.

## Provisional choices and ambiguities

These are implementation defaults, not additional agreed product requirements:

- UUID identifiers and immutable records minimize accidental mutation through shared
  references. Construct replacements and save them to apply updates.
- Timestamps use Instant; future UI formatting should use an explicit zone such as
  Asia/Singapore. Date-filter boundaries and overlap rules still need agreement.
- ConsultationNote is a single editable note keyed by booking ID. A multi-note
  conversation or private tutor-only notes would need a different model.
- Login uses explicitly labelled demo accounts. Password storage, authentication,
  account provisioning, and deployment/security model remain undecided. This demo
  must be replaced before public use; it grants access by account selection.
- Soft deactivation/cancellation preserves history. Rules for deactivating a tutor
  or module with future bookings, deleting booked slots, and unassigning tutors with
  future slots remain undecided.
- SQLite has a reserved package but is not selected or implemented by this change.
- Minimal JUnit/build setup overlaps Person C's planned testing ownership and is
  included so the shared foundation is runnable and verifiable. Person C still owns
  expanding the framework; Person A still owns CI/CD and release packaging.
- The development distribution contains JavaFX dependencies for the host platform.
  The assignment's cross-platform release JAR remains separate release work.
- GitHub uses main; the assignment specifies master for grading. Resolve before
  submission. This implementation does not rename the remote branch.

## Verification

`./gradlew test` runs domain, fake repository, and non-UI session/router tests.
`./gradlew uiTest` opens a JavaFX stage and exercises actual selection, Continue,
role heading, and Sign out for every role. This is a shell smoke test, not the
student booking/cancellation end-to-end test planned for Person A.
The UI test also writes scene images to `build/reports/ui-snapshots/`. The current
classpath setup produces a JavaFX unnamed-module configuration warning during the
UI test; it passed on Windows. macOS/Linux execution has not been verified.

## Dependency references

- Gradle 9.1.0 supports Java 25: https://docs.gradle.org/9.1.0/release-notes.html
- JavaFX Gradle integration: https://github.com/openjfx/javafx-gradle-plugin
- Foojay toolchain resolver: https://github.com/gradle/foojay-toolchains

Generated with Codex assistance; see logs/ for an interaction summary pending human
verification. No third-party application code was copied. The Gradle wrapper is
standard generated Gradle tooling, not original project code.
