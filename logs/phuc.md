# Phuc — repository interaction log

Recorded on 27 September 2026. This cumulative log summarizes every repository-related
user request visible in this conversation, in order. It records work performed with
Codex assistance and distinguishes requests, previews, implementation, and verification.
It does not infer independently authored student work, personal reflections, or
interactions from other conversations. Earlier entries are reconstructed from the
available conversation context and checked against repository files/history where possible.

## 1. Browse and filter available consultation slots

**Request:** Implement browsing of available consultation slots with module, tutor,
and date search/filtering.

**Work and outcome:**

- Added `SlotFilter` and filtering in `StudentService`.
- Added module code/name, tutor name, and date controls to the student's Available
  slots screen, with Search and Clear actions.
- Text matching trims input and ignores case; date matching uses Singapore time (SGT).
- Kept availability rules: future available slots, active tutors/modules, valid tutor
  assignments, and no existing active booking for the slot.
- Extended service and student UI coverage for combined filters and clearing filters.
- Repository evidence: commit `3f24e3d`, `feat(student): add filter and search for student booking`.

The initial table/date-picker presentation was subsequently replaced by the calendar
and timetable in interaction 5; course/tutor filtering remains available.

## 2. Add active modules from the NUS CS curriculum

**Request:** Add a list of active modules based on the NUS CS curriculum.

**Work and outcome:**

- Updated `DemoData` to seed 15 named common, foundation, and mathematics requirements
  from the NUS BComp (CS) AY2026/27 curriculum.
- Courses: CS1101S, ES2660, IS1108, CS1231S, CS2030S, CS2040S, CS2100, CS2101,
  CS2103T, CS2106, CS2109S, CS3230, MA1521, MA1522, and ST2334.
- Used stable course IDs and duplicate checks. Restarting preserves existing course
  edits and deactivations rather than recreating or reactivating those records.
- Added `DemoDataTest` coverage for seeding and preservation of existing data.
- This is a selected curriculum seed, not the full NUS course catalogue or a live
  NUSMods integration. Demo seeding does not create consultation slots or assignments.
- Repository evidence: commit `acdcc1e`, `chore: add more course in demo data`.

Source recorded in the implementation:
[NUS CS AY2026/27 curriculum](https://www.comp.nus.edu.sg/cug/per-cohort/cs/cs-26-27/).

## 3. Filter the student's My bookings screen

**Request:** Add date, course, and tutor filters to My bookings.

**Work and outcome:**

- Added `BookingFilter` and corresponding booking queries in `StudentService`.
- Added labelled course code/name, tutor name, and SGT date controls, plus Search
  and Clear actions, to My bookings.
- Filters are independent of available-slot filters. Refresh retains filter values;
  Clear removes them. Text searches ignore case and surrounding whitespace.
- Kept active and historical bookings visible when they match the selected filters.
- Extended service and UI tests for booking filters and clearing them.
- Repository evidence: commit `af0607c`, `feat(student): add filter for current student booking`.

## 4. Redesign the app using the supplied design reference

**Request:** “Come up with a better UI for the app,” with
`C:\Users\tieut\Downloads\DESIGN.md` supplied as a visual reference.

**Work and outcome:**

- Interpreted the document as design reference material: black backgrounds, violet
  actions, white typography, muted supporting text, and geometric login artwork.
- Added shared `AppUi` helpers, `unichope.css`, and procedural `Constellation` artwork.
- Restyled login and student, tutor, and admin workspaces with shared headers,
  labelled fields, tabs, empty states, focus styles, and action styles.
- Set the initial app size to 1180 × 780, with a minimum window size of 900 × 660
  and compact workspace spacing below 720 pixels in height.
- Arranged tutor history/notes and admin statistics to use horizontal space better.
- Added `docs/UiDesign.md` and screenshot support through `support.UiSnapshots`.

**Verification recorded at this stage:** 104 logic tests and the relevant desktop
UI checks passed. Screenshots were inspected for normal and compact layouts.

This dark visual direction was later superseded by the explicitly requested and
approved MP1 blue-and-white design in interactions 7–8.

## 5. Add a large calendar before NUSMods-inspired slot selection

**Request:** Use NUSMods as the slot-selection design reference, with a large
calendar for selecting a date first.

**Work and outcome:**

- Reviewed available NUSMods reference material. A live browser session was unavailable;
  reference research used the public site, official repository, and blog material.
- Added `StudentSlotBrowser`: a month calendar with availability counts, current-day
  highlighting, disabled past dates, month navigation, and a This month action.
- Selecting a date opens that day's timetable. Course/tutor searches narrow the day;
  Clear keeps the selected date; Calendar returns to the month and clears text filters.
- Added `SlotTimeline`: a horizontal time grid with course/tutor rows and selectable
  consultation blocks. Overlapping choices occupy separate lanes.
- Selection displays the course, tutor, and exact time before enabling Book selected.
- Added `StudentService.today()` using SGT so the current-day boundary matches displayed
  dates. Booking/refresh resets selection and removes unavailable or withdrawn slots.
- Updated the user guide and UI design notes for the two-step workflow.

**Verification and fixes:**

- Added an interval-lane test and updated the student UI scenario for calendar
  navigation, empty days, midnight in SGT, course/tutor filtering, booking,
  cancellation, and slots withdrawn after selection.
- Corrected a test fixture that initially used nonexistent slot accessors by using
  the existing `withStatus` method.
- Used fresh preview scenes to avoid cached-text artifacts in repeated JavaFX snapshots.
- Adjusted compact booking details and the final timeline hour's available width.
- 105 logic tests and the student/login UI checks passed; subsequent targeted student
  checks also passed after visual adjustments.

Reference: [NUSMods timetable](https://nusmods.com/timetable). This work borrows the
time-grid interaction pattern; it does not connect the app to NUSMods data.

## 6. User correction: show a picture before implementation

**Request:** “give me a picture first before implement”.

**Response and outcome:**

- Acknowledged that the preceding calendar/timetable changes had already been implemented.
- Paused further edits and displayed actual calendar and timetable screenshots.
- Agreed to wait for design feedback before making additional implementation changes.
- No code was changed during this preview-only interaction.

## 7. Preview a blue-and-white design based on MP1

**Request:** Use the blue-and-white colour scheme from
`D:\cs3227\CS3227-2610-MP1`.

**Work and outcome:**

- Read MP1's `src/main/resources/css/application.css` and `calendar.css` as references.
- Extracted the palette: royal blue `#2563EB`, white `#FFFFFF`, pale background
  `#F5F6FA`, borders `#E4E7EF`, dark text `#111827`, and navy `#0F172A`.
- Used the imagegen skill and built-in image generation tool to produce a visual
  proposal with the calendar and slot timetable side by side.
- The proposal retained the calendar-first flow, course/tutor filters, and explicit
  booking action. It used white panels, rounded controls, blue highlights, and pale
  blue consultation blocks instead of the earlier dark palette.
- Displayed the proposal and asked whether to apply it. No app code changed in this
  interaction, and the MP1 project was only read.

## 8. Implement the approved blue-and-white design

**Request:** “implement the design”.

**Work and outcome:**

- Replaced the shared dark stylesheet with MP1-derived blue-and-white colours.
- Applied white panels, pale outer backgrounds, navy headings, blue actions, light
  borders, rounded controls, and visible focus states across the app.
- Updated calendars, timetable blocks, selected states, tables, filters, dropdowns,
  date pickers, and confirmation-dialog styles.
- Styled the slot Search action as a blue primary button; retained labelled course
  and tutor filters, slot details, and the explicit Book selected action.
- Added a separator beneath workspace navigation and recoloured login artwork in blue.
- Updated `docs/UiDesign.md` to record the approved direction replacing the dark reference.
- Tightened panel/header spacing after screenshots showed unnecessary calendar and
  timetable clipping at the default window size. Compact layouts retain scrolling
  while keeping the booking details and action accessible.

**Verification:**

- `gradlew.bat uiTest` passed all seven desktop tests: AdminUiTest, ShellUiTest,
  StudentUiTest, TutorAccessUiTest, TutorBookingUiTest, TutorHistoryUiTest, and TutorUiTest.
- Targeted StudentUiTest reruns passed after spacing adjustments.
- Inspected login, admin, tutor, calendar, and timetable screenshots, including compact
  student layouts. No CSS resolution errors were found in the checked test reports.
- `git diff --check` found no whitespace errors. Git emitted line-ending conversion
  notices for some existing modified files.
- The 105-test logic-suite result belongs to interaction 5; it was not rerun for this
  colour/layout-only implementation.

Generated screenshots are local build artifacts in `build/reports/design-snapshots/`,
including `student-calendar.png`, `student-slot-timetable.png`, and their `-small`
variants. These files can be regenerated by the desktop tests and are not tracked.

## 9. Consolidate the interactions in this log

**Request:** Summarize every repository interaction in `logs/phuc.md`.

**Work and outcome:**

- Reviewed the available conversation, repository status, relevant implementation,
  and the three feature commits listed above.
- Created this chronological log, including the preview-before-implementation
  correction, design approval, superseded design, test outcomes, and final state.
- Kept `logs/kokseng.md` separate and unchanged.
- This interaction changes documentation only; no new application test run is required.

## 10. Remove the login footer tagline

**Request:** Remove the "Find a slot" / conversation / next-step tagline.

**Work and outcome:**

- Removed "Find a slot. Start a conversation. Take the next step." from the bottom
  of the login screen in `UniChopeApplication`.
- This is a text-only UI removal; no application test run was needed.

## 11. Draft a student-branch merge summary

**Request:** Write a summary for merging the student branch with `origin/main`.

**Work and outcome:**

- Compared the actual branch, `branch_student`, with the local `origin/main` reference.
- Drafted a summary of student booking, filters, curriculum seeds, SQLite integration,
  UI changes, documentation, and prior verification results.
- Clarified that the UI/calendar changes were still uncommitted and needed to be
  committed to be included in the proposed merge. No merge or commit was performed.

## 12. Recover the UI stash on `branch_UI`

**Request:** Resolve repeated `git stash pop` failures reporting that local changes
to `.gitignore` would be overwritten.

**Work and outcome:**

- Found that the local `.gitignore` edit was identical to the stashed edit.
- Found that `branch_UI` had been created from an older `main`; the local
  `origin/main` reference already contained the student merge at `505dd15`.
- Backed up `.gitignore` and eight untracked files, with SHA-256 hashes, under
  `build/recovery/ui-stash-20260927-203043/`.
- Restored only the duplicated `.gitignore` edit to HEAD, fast-forwarded `branch_UI`
  to `origin/main`, and applied stash `f4d7b29a532830f3f9042a3fdd54ad7282cdbf24`.
- The sandbox initially prevented Git from creating its index lock; the same Git
  recovery operations then succeeded through the approved elevated tool invocation.
- Preserved the original stash and left recovered changes uncommitted for review.

**Verification:** All 16 recovered tracked files matched the stash. All nine local
files matched their backup hashes before this log update. Git reported no unresolved
conflicts, no divergence from `origin/main`, and no whitespace errors. Application
tests were not rerun because this operation restored existing work without changing
application logic.

## State after stash recovery

- Available-slot browsing uses calendar → daily timetable → explicit booking.
- Course/tutor searches and independent My bookings course/tutor/date filters remain.
- Demo data includes the 15 selected curriculum modules described above.
- The current implemented visual direction is the approved MP1 blue-and-white theme.
- The student features are merged in `origin/main`. The current branch is `branch_UI`,
  based on that main revision, with the recovered UI changes in its working tree.
- The original UI stash and the local recovery backup remain available.
- No new commit, push, or pull request was created during stash recovery.
