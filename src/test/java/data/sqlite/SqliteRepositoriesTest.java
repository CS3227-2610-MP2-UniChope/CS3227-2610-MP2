package data.sqlite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationNote;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.module.Module;
import model.module.TutorModule;
import model.user.Student;
import model.user.Tutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shell.DemoData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteRepositoriesTest {
    @TempDir Path directory;

    @Test
    void open_emptyDatabase_persistsUserAfterReopen() {
        Path database = directory.resolve("unichope.db");
        Student student = new Student(UUID.randomUUID(), "Alice", "alice@example.edu", true);

        SqliteRepositories.open(database).users().save(student);

        assertEquals(student, SqliteRepositories.open(database).users()
                .findById(student.id()).orElseThrow());
    }

    @Test
    void open_schemaV1Database_preservesUsersAndConsultations() throws Exception {
        Path database = directory.resolve("legacy.db");
        createSchemaV1Database(database);

        var repositories = SqliteRepositories.open(database);

        UUID studentId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID tutorId = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID moduleId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UUID slotId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID bookingId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        Student student = new Student(studentId, "Alice", "alice@example.edu", true);
        Tutor tutor = new Tutor(tutorId, "Ada", "ada@example.edu", true);
        Module module = new Module(moduleId, "CS3227", "Software Engineering", true);
        ConsultationSlot slot = new ConsultationSlot(slotId, tutorId, moduleId,
                Instant.parse("2026-09-24T01:00:00Z"), Instant.parse("2026-09-24T02:00:00Z"),
                SlotStatus.COMPLETED);
        Booking booking = new Booking(bookingId, studentId, slotId, Instant.EPOCH, BookingStatus.COMPLETED);

        assertEquals(student, repositories.users().findById(studentId).orElseThrow());
        assertEquals(tutor, repositories.users().findById(tutorId).orElseThrow());
        assertEquals(module, repositories.modules().findById(moduleId).orElseThrow());
        assertEquals(List.of(new TutorModule(tutorId, moduleId)),
                repositories.modules().findAssignmentsByTutor(tutorId));
        assertEquals(slot, repositories.slots().findById(slotId).orElseThrow());
        assertEquals(booking, repositories.bookings().findById(bookingId).orElseThrow());
        assertEquals("Completed consultation", repositories.bookings().findNoteByBookingId(bookingId)
                .orElseThrow().content());
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            try (var version = statement.executeQuery("PRAGMA user_version")) {
                assertEquals(2, version.getInt(1));
            }
            try (var user = statement.executeQuery("SELECT password_hash, must_change_password "
                    + "FROM users WHERE id = '10000000-0000-0000-0000-000000000001'")) {
                assertTrue(user.next());
                assertNull(user.getString("password_hash"));
                assertEquals(1, user.getInt("must_change_password"));
            }
            try (var marker = statement.executeQuery("SELECT value FROM application_metadata "
                    + "WHERE key = 'auth_bootstrap_complete'")) {
                assertTrue(marker.next());
                assertEquals("false", marker.getString("value"));
            }
        }
    }

    @Test
    void save_duplicateEmailIgnoringCase_rejectsRequest() {
        Path database = directory.resolve("unichope.db");
        var users = SqliteRepositories.open(database).users();
        users.save(new Student(UUID.randomUUID(), "Alice", "alice@example.edu", true));

        assertThrows(IllegalArgumentException.class, () -> users.save(
                new Student(UUID.randomUUID(), "Other", "ALICE@example.edu", true)));
    }

    @Test
    void saveModule_assignedTutorAfterReopen_persistsModuleAndAssignment() {
        Path database = directory.resolve("unichope.db");
        var repositories = SqliteRepositories.open(database);
        Tutor tutor = new Tutor(UUID.randomUUID(), "Ada", "ada@example.edu", true);
        Module module = new Module(UUID.randomUUID(), "CS3227", "Software Engineering", true);
        repositories.users().save(tutor);
        repositories.modules().save(module);
        TutorModule assignment = new TutorModule(tutor.id(), module.id());
        repositories.modules().assign(assignment);

        var reopened = SqliteRepositories.open(database);

        assertEquals(module, reopened.modules().findById(module.id()).orElseThrow());
        assertEquals(List.of(assignment), reopened.modules().findAssignmentsByTutor(tutor.id()));
        assertThrows(IllegalArgumentException.class, () -> reopened.modules().save(
                new Module(UUID.randomUUID(), "cs3227", "Duplicate", true)));
    }

    @Test
    void assign_unknownTutor_existingModule_preservesAssignment() {
        var repositories = SqliteRepositories.open(directory.resolve("unichope.db"));
        Module module = new Module(UUID.randomUUID(), "CS3227", "Software Engineering", true);
        TutorModule assignment = new TutorModule(UUID.randomUUID(), module.id());
        repositories.modules().save(module);

        repositories.modules().assign(assignment);

        assertEquals(List.of(assignment), repositories.modules().findAssignmentsByTutor(assignment.tutorId()));
    }

    @Test
    void saveSlot_existingTutorAndModule_persistsQueriesAfterReopen() {
        Path database = directory.resolve("unichope.db");
        var repositories = SqliteRepositories.open(database);
        Tutor tutor = new Tutor(UUID.randomUUID(), "Ada", "ada@example.edu", true);
        Module module = new Module(UUID.randomUUID(), "CS3227", "Software Engineering", true);
        ConsultationSlot slot = new ConsultationSlot(UUID.randomUUID(), tutor.id(), module.id(),
                Instant.parse("2026-09-24T01:00:00Z"), Instant.parse("2026-09-24T02:00:00Z"),
                SlotStatus.AVAILABLE);
        repositories.users().save(tutor);
        repositories.modules().save(module);
        repositories.slots().save(slot);

        var reopened = SqliteRepositories.open(database);

        assertEquals(List.of(slot), reopened.slots().findByTutorId(tutor.id()));
        assertEquals(List.of(slot), reopened.slots().findByModuleId(module.id()));
    }

    @Test
    void completeActiveBooking_bookedSlot_completesBothRecords() {
        var records = bookedConsultation();

        Booking completed = records.repositories.lifecycle().completeActiveBooking(records.tutor.id(), records.booking.id());

        assertEquals(BookingStatus.COMPLETED, completed.status());
        assertEquals(BookingStatus.COMPLETED,
                records.repositories.bookings().findById(records.booking.id()).orElseThrow().status());
        assertEquals(SlotStatus.COMPLETED,
                records.repositories.slots().findById(records.slot.id()).orElseThrow().status());
    }

    @Test
    void completeActiveBooking_availableSlot_leavesBothRecordsUnchanged() {
        var records = bookedConsultation();
        records.repositories.slots().save(records.slot.withStatus(SlotStatus.AVAILABLE));

        assertThrows(IllegalArgumentException.class, () -> records.repositories.lifecycle()
                .completeActiveBooking(records.tutor.id(), records.booking.id()));

        assertEquals(BookingStatus.ACTIVE,
                records.repositories.bookings().findById(records.booking.id()).orElseThrow().status());
        assertEquals(SlotStatus.AVAILABLE,
                records.repositories.slots().findById(records.slot.id()).orElseThrow().status());
    }

    @Test
    void withExclusiveAccess_failedOperation_rollsBackChanges() {
        var repositories = SqliteRepositories.open(directory.resolve("unichope.db"));
        Student student = new Student(UUID.randomUUID(), "Rollback", "rollback@example.edu", true);

        assertThrows(IllegalStateException.class, () -> repositories.lifecycle().withExclusiveAccess(() -> {
            repositories.users().save(student);
            throw new IllegalStateException("test rollback");
        }));

        assertTrue(repositories.users().findById(student.id()).isEmpty());
    }

    @Test
    void saveNote_completedBooking_persistsEditedNote() {
        var records = bookedConsultation();
        records.repositories.lifecycle().completeActiveBooking(records.tutor.id(), records.booking.id());
        ConsultationNote first = new ConsultationNote(records.booking.id(), "Discussed testing", Instant.EPOCH);
        ConsultationNote edited = new ConsultationNote(records.booking.id(), "Added follow-up", Instant.ofEpochSecond(60));

        records.repositories.bookings().saveNote(first);
        records.repositories.bookings().saveNote(edited);

        assertEquals(edited, records.repositories.bookings().findNoteByBookingId(records.booking.id()).orElseThrow());
    }

    @Test
    void seed_existingDatabase_preservesExistingUsers() {
        var repositories = SqliteRepositories.open(directory.resolve("unichope.db"));
        Student student = new Student(UUID.randomUUID(), "Existing", "existing@example.edu", true);
        repositories.users().save(student);

        DemoData.seed(repositories);

        assertEquals(List.of(student), repositories.users().findAll());
    }

    private ConsultationRecords bookedConsultation() {
        var repositories = SqliteRepositories.open(directory.resolve("consultation.db"));
        Tutor tutor = new Tutor(UUID.randomUUID(), "Ada", "ada@example.edu", true);
        Student student = new Student(UUID.randomUUID(), "Lin", "lin@example.edu", true);
        Module module = new Module(UUID.randomUUID(), "CS3227", "Software Engineering", true);
        ConsultationSlot slot = new ConsultationSlot(UUID.randomUUID(), tutor.id(), module.id(),
                Instant.parse("2026-09-24T01:00:00Z"), Instant.parse("2026-09-24T02:00:00Z"),
                SlotStatus.BOOKED);
        Booking booking = new Booking(UUID.randomUUID(), student.id(), slot.id(), Instant.EPOCH, BookingStatus.ACTIVE);
        repositories.users().save(tutor);
        repositories.users().save(student);
        repositories.modules().save(module);
        repositories.slots().save(slot);
        repositories.bookings().save(booking);
        return new ConsultationRecords(repositories, tutor, slot, booking);
    }

    private static void createSchemaV1Database(Path database) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE users (id TEXT PRIMARY KEY, name TEXT NOT NULL, "
                    + "email TEXT NOT NULL COLLATE NOCASE UNIQUE, role TEXT NOT NULL, active INTEGER NOT NULL "
                    + "CHECK (active IN (0, 1)))");
            statement.executeUpdate("CREATE TABLE modules (id TEXT PRIMARY KEY, code TEXT NOT NULL "
                    + "COLLATE NOCASE UNIQUE, name TEXT NOT NULL, active INTEGER NOT NULL CHECK (active IN (0, 1)))");
            statement.executeUpdate("CREATE TABLE tutor_modules (tutor_id TEXT NOT NULL, module_id TEXT NOT NULL, "
                    + "PRIMARY KEY (tutor_id, module_id))");
            statement.executeUpdate("CREATE TABLE consultation_slots (id TEXT PRIMARY KEY, tutor_id TEXT NOT NULL, "
                    + "module_id TEXT NOT NULL, start_time TEXT NOT NULL, end_time TEXT NOT NULL, status TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE bookings (id TEXT PRIMARY KEY, student_id TEXT NOT NULL, "
                    + "slot_id TEXT NOT NULL, created_at TEXT NOT NULL, status TEXT NOT NULL, "
                    + "CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED')))");
            statement.executeUpdate("CREATE UNIQUE INDEX active_booking_per_slot ON bookings(slot_id) "
                    + "WHERE status = 'ACTIVE'");
            statement.executeUpdate("CREATE TABLE consultation_notes (booking_id TEXT PRIMARY KEY, "
                    + "content TEXT NOT NULL, updated_at TEXT NOT NULL)");
            statement.executeUpdate("INSERT INTO users VALUES "
                    + "('10000000-0000-0000-0000-000000000001', 'Alice', 'alice@example.edu', 'STUDENT', 1), "
                    + "('10000000-0000-0000-0000-000000000002', 'Ada', 'ada@example.edu', 'TUTOR', 1)");
            statement.executeUpdate("INSERT INTO modules VALUES "
                    + "('20000000-0000-0000-0000-000000000001', 'CS3227', 'Software Engineering', 1)");
            statement.executeUpdate("INSERT INTO tutor_modules VALUES "
                    + "('10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001')");
            statement.executeUpdate("INSERT INTO consultation_slots VALUES "
                    + "('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', "
                    + "'20000000-0000-0000-0000-000000000001', '2026-09-24T01:00:00Z', "
                    + "'2026-09-24T02:00:00Z', 'COMPLETED')");
            statement.executeUpdate("INSERT INTO bookings VALUES "
                    + "('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', "
                    + "'30000000-0000-0000-0000-000000000001', '1970-01-01T00:00:00Z', 'COMPLETED')");
            statement.executeUpdate("INSERT INTO consultation_notes VALUES "
                    + "('40000000-0000-0000-0000-000000000001', 'Completed consultation', '1970-01-01T00:00:00Z')");
            statement.executeUpdate("PRAGMA user_version = 1");
        }
    }

    private record ConsultationRecords(data.repository.Repositories repositories, Tutor tutor,
                                       ConsultationSlot slot, Booking booking) { }
}
