# UniChope user guide

This guide describes the current development app. It uses local SQLite storage at
`~/.unichope/unichope.db`, so changes remain after the app closes. Login is a demo
account selector, not secure authentication. The Tutor workspace is available;
the Student workspace remains incomplete.

## Setup

Follow the [README](../README.md) for JDK prerequisites and Gradle commands. Run
`./gradlew run` on macOS/Linux or `.\gradlew.bat run` on Windows. Select Demo Admin
and Continue. No password is used by the current demo login.

## Admin workspace

Five tabs provide Users, Modules, Assignments, Bookings, and Statistics. Refresh
reloads current data; Sign out returns to account selection. Messages below the
workspace explain validation errors and blocked changes. Hover over truncated table
cells for the full text. Admin actions require an active admin account.

### Users

Enter a name and email, select STUDENT or TUTOR, and press Add user. Names cannot be
blank and email addresses must be valid and unique ignoring case, including inactive
accounts. The table lists students and tutors; admin accounts cannot be managed here.

To remove a user, select the row and press Deactivate selected, then confirm.
Deactivation keeps the record and booking history. There is no permanent deletion or
reactivation action. Students with ACTIVE bookings cannot be deactivated. Tutors
with ACTIVE bookings or future AVAILABLE slots cannot be deactivated. Overdue ACTIVE
bookings also block the action. Resolve these consultations through the relevant
role workflows before retrying; those workflows are not yet available in this branch.

### Modules

Enter a code and name, then Create. Codes are unique ignoring case. Select a row to
populate the form, change its code/name, and press Save selected. Editing preserves
its identity and active/inactive status. Deactivate selected retains history and
requires confirmation; ACTIVE bookings or future AVAILABLE slots for the module
block the action. Existing assignments are retained, but inactive modules cannot
receive new assignments.

### Assignments

Choose an active tutor and active module, then Assign. Repeating the same pair does
not create a duplicate. All existing assignments are listed, including inactive
entities. Select an assignment and choose Unassign selected, then confirm. ACTIVE
bookings or future AVAILABLE slots for that tutor-module pair block unassignment;
consultations for unrelated pairs do not. History is not deleted.

### Bookings

The table displays every booking status with student, tutor, module, start/end time,
and status. Times are shown in Singapore time (SGT). Default ordering is chronological;
column headers can change the table sort. Inactive entities remain visible. Missing
references display Unavailable rather than hiding the booking. This view is read-only
and does not display consultation notes.

### Statistics

Statistics cover all time, including inactive entities and all booking statuses.
Counts are shown per module and tutor for groups with bookings; groups with missing
references appear as Unknown. Tutor emails distinguish tutors with the same name.

- Completion rate = completed bookings / all bookings × 100.
- Cancellation rate = cancelled bookings / all bookings × 100.
- Zero bookings gives 0.0% for both rates; percentages display one decimal place.
- A cancelled booking followed by another booking counts as two booking records.

Press Refresh after data changes. The demo starts without bookings or modules, so
those screens initially show no records. The admin cannot create bookings.

## Verification

Run `./gradlew test` (Windows: `.\gradlew.bat test`) for logic tests. Run `uiTest`
on a graphical desktop for admin forms, confirmation handling, blocked changes,
booking/statistics refresh, revoked access, and three-role login/sign-out regression.
The UI tests use independent fixtures; their sample bookings do not populate the app.
