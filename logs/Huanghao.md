# Huanghao — Development Summary

## 1. Service design and lifecycle contract

### Establish TutorService scope

- **Request:** Read the project instructions and shared foundation, then plan TutorService
  tests before building the tutor UI.
- **Outcome:** Defined the boundary: JavaFX calls TutorService; the service owns tutor
  authorization and business rules; repositories remain behind shared data contracts.
- **Check / test:** Read `AGENTS.md`, `PROJECT_REQUIREMENTS.md`, `PROJECT_PLAN.md`, and
  `docs/Foundation.md`; checked the merged foundation and repository contracts.

### Agree slot and tutor rules

- **Request:** Decide how inactive tutors/modules, past slots, overlapping slots, and
  rescheduling should behave before implementation.
- **Outcome:** New actions are rejected for inactive tutors. New slots require an active
  assigned module, a non-past start, and no overlap with AVAILABLE or BOOKED slots;
  adjacent intervals are allowed. Rescheduling was deferred. Tutors may cancel AVAILABLE
  slots, but a booked slot requires student cancellation first.
- **Check / test:** Recorded the rules in the service design and mapped them to planned
  TutorService cases before implementation.

### Agree completion and history rules

- **Request:** Define tutor completion timing, valid lifecycle states, and history access.
- **Outcome:** A tutor may complete before the scheduled end, but only for an `ACTIVE`
  booking paired with a `BOOKED` slot. Both change atomically to `COMPLETED`. Completed
  consultations and notes remain readable after tutor deactivation.
- **Check / test:** Lifecycle tests first exposed the missing shared operation and invalid
  state handling. Focused lifecycle tests and `./gradlew test` passed after implementation.

## 2. Slot creation and service test foundation

### Align service with shared conventions

- **Request:** Use the shared session, repository, clock, and logging conventions without
  trusting a caller-supplied tutor identity.
- **Outcome:** Bound operations to the signed-in actor and revalidated the tutor inside the
  exclusive operation boundary using `Repositories`, `Clock`, and `OperationLog`.
- **Check / test:** TutorService tests exercise active tutor authorization and rejected
  actions; detailed red/green evidence is in the full log.

### Create valid consultation slots

- **Request:** Allow an eligible tutor to create a slot while rejecting invalid scheduling
  requests.
- **Outcome:** Create AVAILABLE slots only for active tutors assigned to active modules;
  reject past starts and overlap with AVAILABLE or BOOKED slots. Cancelled/completed slots
  do not block scheduling, and endpoint-adjacent slots are accepted.
- **Check / test:** Added `TutorFixture` and cases for missing/inactive tutor, inactive
  module, missing assignment, past time, overlap, adjacency, historical slots, and
  concurrent duplicate creation. Focused `TutorServiceTest` and `./gradlew test` passed;
  committed as `1d6446d`.

## 3. Tutor booking, history, and note services

### Add upcoming-slot queries and cancellation

- **Request:** Let tutors review their upcoming slots and cancel eligible ones.
- **Outcome:** Added tutor-owned cancellation and upcoming lookup. Results use the selected
  Singapore-local date, sort by start time, and include only AVAILABLE or BOOKED slots.
- **Check / test:** TutorService cases cover date filtering, allowed statuses, ownership,
  and slot state; focused and full service tests passed.

### Add booking filters and UI-ready rows

- **Request:** Provide tutor-owned booking results for filtering and display without
  exposing repositories to JavaFX.
- **Outcome:** Added `BookingFilter` criteria for module, Singapore-local date, and status.
  Kept `findBookings` as the raw domain query and added `findBookingViews` containing the
  student, module, slot times, and status for the UI table.
- **Check / test:** Service tests cover filtering and view-row mapping; shared repository
  contracts were unchanged.

### Add completion and consultation notes

- **Request:** Complete a booking and retain tutor access to consultation notes/history.
- **Outcome:** Completion uses the shared atomic lifecycle operation and rejects invalid
  ownership or inconsistent states without partial updates. Tutors can create, read, or
  replace a note for an owned completed booking. Inactive tutors retain completed history
  access only.
- **Check / test:** Tests cover valid/invalid lifecycle pairs, note ownership, and history
  access for inactive tutors; focused and full TutorService tests passed.

### Add module choices and separate history filters

- **Request:** Supply active module choices and keep cancelled/completed history distinct
  from upcoming slots.
- **Outcome:** Added `findActiveAssignedModules()` and `findSlotHistory(date, status)`.
  History accepts CANCELLED or COMPLETED; upcoming results remain AVAILABLE or BOOKED.
- **Check / test:** Tests cover inactive tutor/module behavior, status validation, and
  completed history access. Focused and full TutorService tests passed; no repository
  contract changed.

## 4. SQLite/JDBC persistence

### Implement SQLite behind repository contracts

- **Request:** Replace in-memory storage with local persistence without changing repository
  interfaces or allowing JavaFX to access SQL directly.
- **Outcome:** Added a shared SQLite/JDBC coordinator and repository bundle for users,
  modules, assignments, slots, bookings, and notes. Lifecycle operations share a database
  transaction so completion cannot partially update booking and slot state.
- **Check / test:** Temporary-database tests cover reopen persistence, repository behavior,
  uniqueness, rollback, and atomic completion. Focused SQLite tests and `./gradlew test`
  passed.

### Wire application startup and database location

- **Request:** Use one local database for the whole application and avoid reseeding
  accounts on every launch.
- **Outcome:** Startup opens `data/unichope.db`; demo accounts are seeded only when the
  user store is empty. Database and sidecar files are ignored by Git. General migration
  support was left out; unsupported schema versions are rejected.
- **Check / test:** A regression test checks that existing users are preserved rather
  than reseeded. `./gradlew test` and `./gradlew uiTest` passed, as recorded in the log.

## 5. JavaFX Tutor workspace

### Build the Slots tab

- **Request:** Replace the tutor placeholder with a workspace for creating and cancelling
  slots using assigned active modules.
- **Outcome:** Added a Java-built JavaFX workspace matching the Admin UI conventions. The
  Slots tab provides module/date/time controls and an upcoming table; input and display
  use `Asia/Singapore`. FXML was not introduced.
- **Check / test:** `TutorUiTest` exercises workspace controls and slot creation/cancellation;
  focused UI and full tests passed.

### Add bookings and completion

- **Request:** Let tutors filter their bookings and complete a selected booking.
- **Outcome:** Added booking filters and a `TutorBookingView` table. Completion calls
  TutorService and refreshes the workspace.
- **Check / test:** `TutorBookingUiTest` covers filtering and completing a selected booking;
  focused UI and full tests passed.

### Add history and notes

- **Request:** Let tutors inspect cancelled/completed slots and manage notes for completed
  consultations.
- **Outcome:** Added History & Notes views for slot history, completed bookings, and
  loading/saving consultation notes. Recoverable service/security errors appear in the UI.
- **Check / test:** `TutorHistoryUiTest` covers the history/note workflow; focused UI and
  full tests passed. All workspace data/actions route through TutorService, not repositories.

## 6. Authentication and documentation

### Describe account creation and login

- **Request:** Explain how each role obtains an account and how login determines role.
- **Outcome:** Documented that students register themselves, while an existing Admin
  provisions Tutor/Admin accounts and initial passwords. Role is read from SQLite rather
  than selected by the user.
- **Check / test:** Compared guide statements with authentication, tutor, SQLite, and
  startup behavior.

### Expand the Developer Guide

- **Request:** Make storage and tutor architecture understandable to future developers.
- **Outcome:** Explained authentication routing, service/repository boundaries, database
  location, startup seeding, and transactions. Added authentication flow and tutor class
  diagrams, plus a tutor completion activity diagram.
- **Check / test:** Reviewed diagram scope with the user. Technical accuracy and screenshots
  remain subject to human review.

### Update User Guide FAQs and release guidance

- **Request:** Add practical answers about accounts and tutor workflows, and remove
  implementation-phase migration explanations.
- **Outcome:** Added FAQs on local persistence, account provisioning/password reset,
  modules, slots, history, and unavailable rescheduling. Removed obsolete migration/version
  history descriptions.
- **Check / test:** Checked the guide against intended released behavior and current code,
  rather than documenting an upgrade path that will not be supported.
