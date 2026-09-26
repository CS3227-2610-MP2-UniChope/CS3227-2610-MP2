package data.sqlite;

import java.nio.file.Path;
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

    private record ConsultationRecords(data.repository.Repositories repositories, Tutor tutor,
                                       ConsultationSlot slot, Booking booking) { }
}
