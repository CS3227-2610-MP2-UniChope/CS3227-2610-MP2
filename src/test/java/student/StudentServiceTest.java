package student;

import data.repository.Repositories;
import data.sqlite.SqliteRepositories;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.module.Module;
import model.module.TutorModule;
import model.user.Student;
import model.user.Tutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.OperationLog;
import static org.junit.jupiter.api.Assertions.*;

class StudentServiceTest {
    @TempDir Path directory;
    private Repositories data;
    private final Instant now = Instant.parse("2026-09-26T02:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final UUID studentId = UUID.randomUUID();
    private final UUID tutorId = UUID.randomUUID();
    private final UUID moduleId = UUID.randomUUID();
    private StudentService student;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        data = SqliteRepositories.open(directory.resolve("student.db"));
        student = service(studentId);
    }

    private StudentService service(UUID id) {
        return new StudentService(data, id, clock, new OperationLog(clock, event -> { }));
    }

    private void setup() {
        data.users().save(new Student(studentId, "Student", "student@example.edu", true));
        data.users().save(new Tutor(tutorId, "Tutor", "tutor@example.edu", true));
        data.modules().save(new Module(moduleId, "CS1", "Module", true));
        data.modules().assign(new TutorModule(tutorId, moduleId));
    }

    private ConsultationSlot slot(long startSeconds) {
        return data.slots().save(new ConsultationSlot(UUID.randomUUID(), tutorId, moduleId,
                now.plusSeconds(startSeconds), now.plusSeconds(startSeconds + 1800), SlotStatus.AVAILABLE));
    }

    @Test
    void bookingAndCancellationUpdateBothRecordsAndPreserveHistory() {
        setup();
        var first = slot(3600);
        assertEquals(1, student.load().available().size());
        var booking = student.book(first.id());
        assertEquals(BookingStatus.ACTIVE, booking.status());
        assertEquals(SlotStatus.BOOKED, data.slots().findById(first.id()).orElseThrow().status());
        assertTrue(student.load().available().isEmpty());
        assertEquals(1, student.load().bookings().size());
        assertThrows(IllegalArgumentException.class, () -> student.book(first.id()));

        var cancelled = student.cancel(booking.id());
        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        assertEquals(SlotStatus.AVAILABLE, data.slots().findById(first.id()).orElseThrow().status());
        assertEquals(1, student.load().available().size());
        assertThrows(IllegalArgumentException.class, () -> student.cancel(booking.id()));
        var replacement = student.book(first.id());
        assertNotEquals(booking.id(), replacement.id());
        assertEquals(2, data.bookings().findBySlotId(first.id()).size());
    }

    @Test
    void rejectsOverlapsOwnershipAndInactiveStudents() {
        setup();
        var first = slot(3600);
        var overlap = slot(4200);
        var adjacent = slot(5400);
        var booking = student.book(first.id());
        assertThrows(IllegalArgumentException.class, () -> student.book(overlap.id()));
        assertEquals(SlotStatus.AVAILABLE, data.slots().findById(overlap.id()).orElseThrow().status());
        assertEquals(BookingStatus.ACTIVE, student.book(adjacent.id()).status());
        UUID otherId = UUID.randomUUID();
        data.users().save(new Student(otherId, "Other", "other@example.edu", true));
        assertThrows(IllegalArgumentException.class, () -> service(otherId).cancel(booking.id()));
        assertEquals(BookingStatus.ACTIVE, data.bookings().findById(booking.id()).orElseThrow().status());
        data.users().save(data.users().findById(studentId).orElseThrow().withActive(false));
        assertThrows(SecurityException.class, () -> student.cancel(booking.id()));
        assertThrows(SecurityException.class, student::load);
    }

    @Test
    void rejectsPastSlotsAndInactiveOrUnassignedEntities() {
        setup();
        var past = slot(-60);
        assertThrows(IllegalArgumentException.class, () -> student.book(past.id()));
        var future = slot(3600);
        data.modules().unassign(new TutorModule(tutorId, moduleId));
        assertTrue(student.load().available().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> student.book(future.id()));
        data.modules().assign(new TutorModule(tutorId, moduleId));
        data.users().save(data.users().findById(tutorId).orElseThrow().withActive(false));
        assertThrows(IllegalArgumentException.class, () -> student.book(future.id()));
    }

    @Test
    void bookingCannotBeCancelledAfterItStarts() {
        setup();
        var bookedSlot = slot(3600);
        var booking = student.book(bookedSlot.id());
        Clock later = Clock.fixed(now.plusSeconds(3600), ZoneOffset.UTC);
        var laterService = new StudentService(data, studentId, later,
                new OperationLog(later, event -> { }));
        assertThrows(IllegalArgumentException.class, () -> laterService.cancel(booking.id()));
        assertEquals(BookingStatus.ACTIVE, data.bookings().findById(booking.id()).orElseThrow().status());
        assertEquals(SlotStatus.BOOKED, data.slots().findById(bookedSlot.id()).orElseThrow().status());
    }

    @Test
    void concurrentStudentsCannotBookTheSameSlot() throws Exception {
        setup();
        UUID otherId = UUID.randomUUID();
        data.users().save(new Student(otherId, "Other", "other@example.edu", true));
        var available = slot(3600);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> {
                start.await();
                try { student.book(available.id()); return true; }
                catch (IllegalArgumentException rejected) { return false; }
            });
            var second = pool.submit(() -> {
                start.await();
                try { service(otherId).book(available.id()); return true; }
                catch (IllegalArgumentException rejected) { return false; }
            });
            start.countDown();
            assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, data.bookings().findBySlotId(available.id()).size());
        assertEquals(SlotStatus.BOOKED, data.slots().findById(available.id()).orElseThrow().status());
    }
}
