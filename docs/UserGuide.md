# UniChope User Guide

UniChope is a desktop app for booking student consultations. Admins manage
modules, tutors, and users; tutors offer consultation times; students choose
an available time and track their bookings. Dates and times shown in the app
use Singapore time (SGT).

## 1. Quick start

### 1.1 Requirements and launch

The application requires Java 25 and a graphical desktop. Its minimum window
size is 900 × 660. From the repository root, run:

```powershell
.\gradlew.bat run
```

On macOS or Linux, use `./gradlew run`. The first Gradle run may need internet
access to obtain the Java toolchain and dependencies. See the
[README](../README.md) for build and testing commands.

UniChope stores data in `data/unichope.db` relative to the directory from
which it runs. Bookings and edits remain after closing and reopening the app.

### 1.2 Choose a demo account

Select **Demo Admin**, **Demo Tutor**, or **Demo Student** from the account
selector, then press **Continue**. Use **Sign out** in a workspace to return
to the selector. This is a demo login: it does not ask for or verify a password.

A new database contains these three accounts and 15 active modules selected
from the [NUS CS AY2026/27 curriculum](https://www.comp.nus.edu.sg/cug/per-cohort/cs/cs-26-27/).
It starts without tutor assignments, consultation slots, or bookings. Existing
module edits and deactivations are preserved when the app restarts.

## 2. Interface tour

| Workspace | Main tabs | What you can do |
| --- | --- | --- |
| Admin | Users, Modules, Assignments, Bookings, Statistics | Manage the consultation setup and review activity |
| Tutor | Slots, Bookings, History & Notes | Offer time slots, complete bookings, and maintain notes |
| Student | Available slots, My bookings | Find and book a slot, or review and cancel a booking |

**Refresh** reloads stored data. Action messages appear in the workspace and
explain validation errors. Long table values can be inspected by hovering
over their cells in the admin workspace.

## 3. First consultation walkthrough

These steps exercise the main workflow using a fresh demo database.

1. Sign in as **Demo Admin**. Open **Assignments**, choose **Demo Tutor** and
   an active module such as `CS1101S`, then press **Assign**.
2. Sign out and enter as **Demo Tutor**. Open **Slots**, select a future date
   and the assigned module, enter a start and end time in `HH:mm` format
   such as `10:00` and `10:30`, then press **Create**.
3. Sign out and enter as **Demo Student**. In **Available slots**, choose
   the slot's date on the month calendar. Select its coloured block in the
   timetable, check the details below it, then press **Book selected**.
4. Open **My bookings** to see the ACTIVE booking.
5. Sign out and enter as **Demo Tutor**. In **Bookings**, select the booking
   and press **Complete selected** after the consultation. The completed
   booking appears in **History & Notes**, where you can select it, write a
   note, and press **Save note**.
6. Sign in as **Demo Admin** and open **Bookings** or **Statistics** to
   review the resulting records.

Times entered by the tutor and all dates shown to students are interpreted in
SGT. Choose a date and start time that have not already passed.

## 4. Student workspace

### 4.1 Find and book a slot

The **Available slots** tab opens a large month calendar. Days show how many
available consultations they contain. Use the month arrows or **This month**
to navigate. Past dates are disabled. Select a date to open its timetable;
you can also choose a future day with zero slots to see the empty state.

The timetable places time across the top and courses and tutors down the
side. Each coloured block represents one available slot. Scroll horizontally
for later hours and vertically when many tutors or courses are present.
Overlapping choices appear separately so each can be selected.

Use the **Course** and **Tutor** fields to search within the selected day.
Course search accepts part of a code or name; tutor search accepts part of a
name. Matching ignores case and extra surrounding spaces. Press **Search**
or Enter to apply the text filters. **Clear** removes the text filters while
keeping the chosen date. The day arrows change the date, and **Calendar**
returns to the month view.

Select a block to display its course, tutor, and exact time. **Book selected**
becomes available after selection. The app checks availability again when
you book, so a slot taken or withdrawn since the last refresh may be rejected.
You also cannot hold two ACTIVE bookings that overlap in time.

**Refresh** reloads availability and clears the selected block while keeping
the chosen date and any course/tutor search. Calendar counts and day results
use the SGT date on which a slot starts.

### 4.2 Review, filter, and cancel bookings

**My bookings** shows ACTIVE, CANCELLED, and COMPLETED bookings. Search by
part of a course code/name, tutor name, or SGT date; combine fields to narrow
the list. These filters are independent of Available slots. **Clear** resets
all three booking filters, and **Refresh** keeps the current filter values.

To cancel, select an ACTIVE booking and press **Cancel selected**, then
confirm. Cancellation is allowed only before the slot starts. The booking
remains in your history as CANCELLED and the slot becomes available again.

## 5. Tutor workspace

### 5.1 Offer or cancel slots

In **Slots**, choose a date, an active module assigned to your tutor account,
and start/end times in 24-hour `HH:mm` format. Press **Create**. The slot
must start in the future, end after it starts, and not overlap another
AVAILABLE or BOOKED slot owned by you. The table shows your upcoming
AVAILABLE and BOOKED slots for the selected date.

Select an AVAILABLE slot and press **Cancel selected** to withdraw it.
A BOOKED slot cannot be withdrawn this way; its booking must be resolved
through the relevant booking workflow.

### 5.2 Filter and complete bookings

The **Bookings** tab can filter by module, date, and status. Choose the
values and press **Filter**. Select an ACTIVE booking for your slot and press
**Complete selected**. Completing the consultation updates both the booking
and the slot to COMPLETED.

### 5.3 Review history and notes

In **History & Notes**, choose a date and CANCELLED or COMPLETED slot status,
then press **Filter history** to inspect slot history. The completed bookings
table lists your completed consultations. Select one to **Load note**, edit
the note text, or **Save note**. Notes belong to a booking and are stored
with the rest of the local data.

## 6. Admin workspace

The admin workspace has five tabs. Admin actions require an active admin
account. Tables retain inactive records and booking history when relevant.

### 6.1 Users

Enter a name and email, choose STUDENT or TUTOR, and press **Add user**.
Names must not be blank. Email addresses must be valid and unique without
regard to case, including among inactive accounts. Admin accounts cannot be
created or managed here.

Select a user and press **Deactivate selected**, then confirm. Deactivation
retains the record and its history; it does not permanently delete the user.
A student with an ACTIVE booking cannot be deactivated. A tutor with an
ACTIVE booking or future AVAILABLE slot cannot be deactivated. Overdue
ACTIVE bookings still block the action.

### 6.2 Modules

Enter a code and name, then press **Create**. Codes are unique without
regard to case. Select a module to populate the form, edit its code or name,
and press **Save selected**. Its identity and active status are preserved.

**Deactivate selected** requires confirmation. ACTIVE bookings or future
AVAILABLE slots for that module block deactivation. Assignments remain in
history, but inactive modules cannot receive new assignments.

The fresh demo database seeds these modules:

| Computing and communication | Mathematics and statistics |
| --- | --- |
| CS1101S, CS1231S, CS2030S, CS2040S | MA1521, MA1522 |
| CS2100, CS2101, CS2103T, CS2106 | ST2334 |
| CS2109S, CS3230, ES2660, IS1108 |  |

These are selected curriculum requirements, not a live catalogue of all
NUS courses.

### 6.3 Assignments

Choose an active tutor and active module, then press **Assign**. Repeating
the same pair does not create a duplicate. Select an assignment and press
**Unassign selected** to remove it after confirmation. An affected ACTIVE
booking or future AVAILABLE slot blocks unassignment; other tutor-module
pairs do not block it.

### 6.4 Bookings and statistics

**Bookings** is a read-only table of all booking statuses, students, tutors,
modules, and SGT start/end times. Default ordering is chronological; click
column headers to change table sorting. Inactive entities remain visible.
Missing references appear as `Unavailable`.

**Statistics** covers all booking records, including inactive entities and
all statuses. It lists booking counts by module and tutor. The completion
rate is completed bookings divided by all bookings; the cancellation rate
is cancelled bookings divided by all bookings. With no bookings, both rates
show `0.0%`. A cancelled booking followed by a new booking counts as two
records.

## 7. Saving and troubleshooting

Successful changes are stored in `data/unichope.db` as they happen. There is
no separate Save button for the whole application. Closing the window does
not discard completed writes. The database is tied to the working directory;
starting the app from another folder may open a different `data/unichope.db`.

### No consultation slots appear

1. Check that an admin assigned the tutor to an active module.
2. Ask the tutor to create a future AVAILABLE slot.
3. Check the selected SGT date and clear course/tutor filters.
4. Press **Refresh**. Booked, cancelled, past, or withdrawn slots do not
   appear as available.

### A booking or management action is rejected

Read the message in the workspace. A booking may have been taken, withdrawn,
or overlapped with another ACTIVE booking; refresh and choose another time.
Admin changes can be blocked by ACTIVE bookings or future AVAILABLE slots.
Only a future ACTIVE booking can be cancelled by its student.

### The app does not start or prior data is missing

Check `java -version` and confirm the configured Java 25 toolchain is
available. Run the Gradle command from the repository root. Check the
working directory if a previous database appears to be missing. An
unsupported SQLite schema version prevents the app from opening that file;
this version has no automatic schema migration.

## 8. Current limitations

- The login selector is for demonstrations and provides no password
  authentication.
- All data is local. There is no shared server, live NUS course feed, or
  NUSMods account integration.
- The seeded modules are a selected list; assignments and slots must be
  created in the app before a student can make a booking.
- `installDist` builds a development distribution for the current platform;
  a cross-platform release JAR has not been validated here.
