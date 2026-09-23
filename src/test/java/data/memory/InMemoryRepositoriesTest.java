package data.memory;

import data.repository.Repositories;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.List;
import model.consultation.*;
import model.module.Module;
import model.module.TutorModule;
import model.user.Student;
import model.user.Tutor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryRepositoriesTest {
    private final Repositories repositories = InMemoryRepositories.create();

    @Test
    void userUpsertAndSnapshotsPreserveIsolation() {
        Student student = new Student(UUID.randomUUID(), "Alice", "alice@example.edu", true);
        repositories.users().save(student);
        var snapshot = repositories.users().findAll();
        repositories.users().save(student.withActive(false));
        assertEquals(1, repositories.users().findAll().size());
        assertFalse(repositories.users().findById(student.id()).orElseThrow().isActive());
        assertTrue(snapshot.getFirst().isActive());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertTrue(InMemoryRepositories.create().users().findAll().isEmpty());
        assertTrue(repositories.users().findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void usersHaveUniqueEmailsAndStableRoles() {
        Student student = new Student(UUID.randomUUID(), "Alice", "alice@example.edu", true);
        repositories.users().save(student);
        assertEquals(student, repositories.users().findByEmail(" ALICE@example.edu ").orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> repositories.users().save(
                new Tutor(UUID.randomUUID(), "Other", "ALICE@example.edu", true)));
        assertThrows(IllegalArgumentException.class, () -> repositories.users().save(
                new Tutor(student.id(), "Alice", "alice@example.edu", true)));
    }

    @Test
    void assignmentsAreIdempotentAndCanBeRemovedWithoutDeletingModuleHistory() {
        Module module = new Module(UUID.randomUUID(), "CS3227", "Software Engineering", true);
        repositories.modules().save(module);
        TutorModule assignment = new TutorModule(UUID.randomUUID(), module.id());
        repositories.modules().assign(assignment);
        repositories.modules().assign(assignment);
        assertEquals(List.of(assignment), repositories.modules().findAssignmentsByTutor(assignment.tutorId()));
        assertThrows(IllegalArgumentException.class, () -> repositories.modules().save(
                new Module(UUID.randomUUID(), "cs3227", "Duplicate", true)));
        assertThrows(IllegalArgumentException.class, () -> repositories.modules().assign(
                new TutorModule(assignment.tutorId(), UUID.randomUUID())));
        repositories.modules().save(module.withActive(false));
        assertTrue(repositories.modules().unassign(assignment));
        assertFalse(repositories.modules().unassign(assignment));
        assertTrue(repositories.modules().findAssignments().isEmpty());
        assertTrue(repositories.modules().findById(module.id()).isPresent());
    }

    @Test
    void slotsCanBeQueriedByTutorAndModule() {
        var slot = new ConsultationSlot(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1800), SlotStatus.AVAILABLE);
        repositories.slots().save(slot);
        assertEquals(List.of(slot), repositories.slots().findByTutorId(slot.tutorId()));
        assertEquals(List.of(slot), repositories.slots().findByModuleId(slot.moduleId()));
        assertTrue(repositories.slots().findByTutorId(UUID.randomUUID()).isEmpty());
        repositories.slots().save(slot.withStatus(SlotStatus.CANCELLED));
        assertEquals(SlotStatus.CANCELLED, repositories.slots().findById(slot.id()).orElseThrow().status());
    }

    @Test
    void notesRequireCompletedBookingsAndSupportEditing() {
        Booking booking = new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.EPOCH, BookingStatus.ACTIVE);
        var note = new ConsultationNote(booking.id(), "Discussed testing", Instant.EPOCH);
        assertThrows(IllegalArgumentException.class, () -> repositories.bookings().saveNote(note));
        repositories.bookings().save(booking);
        assertThrows(IllegalArgumentException.class, () -> repositories.bookings().saveNote(note));
        Booking completed = booking.withStatus(BookingStatus.COMPLETED);
        repositories.bookings().save(completed);
        repositories.bookings().saveNote(note);
        var edited = new ConsultationNote(booking.id(), "Added follow-up", Instant.EPOCH.plusSeconds(60));
        repositories.bookings().saveNote(edited);
        assertEquals(edited, repositories.bookings().findNoteByBookingId(booking.id()).orElseThrow());
        assertEquals(List.of(completed), repositories.bookings().findByStudentId(booking.studentId()));
        assertThrows(IllegalArgumentException.class, () -> repositories.bookings().save(booking));
    }

    @Test
    void concurrentSavesCannotCreateTwoActiveBookingsForOneSlot() throws Exception {
        UUID slot = UUID.randomUUID();
        Callable<Boolean> attempt = () -> {
            try {
                repositories.bookings().save(new Booking(UUID.randomUUID(), UUID.randomUUID(),
                        slot, Instant.EPOCH, BookingStatus.ACTIVE));
                return true;
            } catch (IllegalArgumentException conflict) {
                return false;
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(attempt, attempt));
            assertEquals(1, results.stream().filter(result -> {
                try { return result.get(); } catch (Exception failure) { throw new AssertionError(failure); }
            }).count());
        }
        Booking first = repositories.bookings().findBySlotId(slot).getFirst();
        repositories.bookings().save(first.withStatus(BookingStatus.CANCELLED));
        assertTrue(attempt.call());
        assertEquals(2, repositories.bookings().findBySlotId(slot).size());
    }
}
