# UniChope — University Consultation Booking System

User-supplied current plan, saved on 23 September 2026. This records intended scope and ownership, not completed implementation. Read alongside PROJECT_REQUIREMENTS.md.

## Shared foundation

Build together first, briefly, because the role implementations depend on it.

- Domain classes: Student, Tutor, Admin, Module, ConsultationSlot, Booking, ConsultationNote.
- Common user abstraction for students, tutors, and admins, with fields: id, name, email, role, isActive.
- TutorModule represents the assignment between tutors and modules.
- Booking status enum: ACTIVE, CANCELLED, COMPLETED.
- Slot status enum: AVAILABLE, BOOKED, CANCELLED, COMPLETED.
- Repository/DAO interfaces first: UserRepository, ModuleRepository, SlotRepository, BookingRepository.
- In-memory fake implementations so each person can build without waiting for real persistence.
- JavaFX shell: login screen and routing to each role's main view based on user type.

Package boundaries match merged PR #1 (initial architecture). Use these names without a `com.app` prefix:

```text
src/main/java/
  model/
    user/          user abstraction, Student, Tutor, Admin
    module/        module-related domain models
    consultation/  consultation-related domain models
  data/
    repository/    shared repository interfaces
    memory/        in-memory implementations
    sqlite/        reserved for SQLite persistence implementations
  student/         student features and UI
  tutor/           tutor features and UI
  admin/           admin features and UI
  shell/           login, routing, application startup
  util/            logging and shared utilities only
src/main/resources/
```

Java package declarations and imports match these paths, e.g. `model.user` and `data.repository`. Tests mirror them under `src/test/java/`. PR #1 provided empty placeholders; the shared foundation is now implemented on feature branch `codex/shared-foundation` with models, repository interfaces/fakes, a demo login/routing shell, and minimal build/test setup. See `docs/Foundation.md` for contracts and provisional decisions. Check GitHub for its current PR/merge status. The `data/sqlite/` folder remains reserved; persistence alternatives remain open until confirmed.

Repository: https://github.com/CS3227-2610-MP2-UniChope/CS3227-2610-MP2. PR #1 was approved and merged into `main` on 23 September 2026. The current default branch is `main`; the assignment specifies `master` for grading, so this discrepancy still needs resolution before submission.

## Person A — Student role

- Browse available consultation slots, filtering/searching by module, tutor, and date.
- Book a slot.
- Cancel a booking.
- View upcoming consultations.
- View consultation history.
- View consultation notes for completed consultations.
- Clearly handle booking failures, such as a slot just booked by another student.
- Implement student-facing screens/controllers and unit tests for student logic.
- Provide at least one end-to-end test for booking and cancellation.
- Also owns CI/CD: GitHub Actions to build, run tests, and package the JAR on every push/PR.

## Person B — Tutor role

- Create available slots, only for modules assigned to the tutor.
- Delete slots.
- View bookings for their slots.
- Mark an active booking as completed.
- Add or edit a consultation note for a completed booking.
- Filter bookings by module, date, and status.
- View upcoming slots and bookings in a date-filtered list.
- View past completed consultations and their notes.
- Implement tutor-facing screens/controllers and unit tests for tutor logic.
- Also owns real persistence (JDBC/SQLite or file-based), implementing the shared repository interfaces and replacing the fakes when ready. The persistence option is not yet selected.

## Person C — Admin role

- Add/remove students and tutors.
- Manage modules: create, edit, deactivate/delete.
- View tutor-module assignments and assign/unassign tutors.
- View all bookings system-wide.
- Prefer deactivating users/modules over deleting records with booking history.
- View basic statistics: bookings per module, bookings per tutor, cancellation rate, and completion rate.
- Implement admin-facing screens/controllers and unit tests for admin logic.
- Also owns testing framework setup (JUnit configuration and test folder structure), plus basic logging/monitoring utilities used by all three roles.

## Shared documentation and outputs

Split ownership rather than co-editing:

- docs/UserGuide.md: each person writes their own role's section.
- docs/DeveloperGuide.md: each person writes their own role's design section; one person owns the architecture overview and acknowledgements (owner not yet specified).
- docs/Reflections.md: each person records their own AI-agent usage as they go.
- logs/: each person exports their own AI interaction summaries into a separate file; summaries must be verified as required by the assignment.
- GitHub Pages site and release tagging: whoever is free first; can proceed independently of the other work.

Actual names for Persons A, B, and C have not yet been provided.
