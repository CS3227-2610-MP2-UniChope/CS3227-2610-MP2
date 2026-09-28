package data.sqlite;

import data.repository.Repositories;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.module.Module;
import model.module.TutorModule;
import model.user.Student;
import model.user.Tutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqliteConsultationLifecycleContractTest {
    @TempDir Path directory;
    private Repositories repositories;

    @BeforeEach
    void setUp() { repositories = SqliteRepositories.open(directory.resolve("lifecycle.db")); }

    @Test
    void completeActiveBooking_activeBookedPair_completesBothRecords() {
        UUID tutorId = UUID.randomUUID();
        ConsultationSlot slot = new ConsultationSlot(UUID.randomUUID(), tutorId, UUID.randomUUID(),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1800), SlotStatus.BOOKED);
        Booking booking = new Booking(UUID.randomUUID(), UUID.randomUUID(), slot.id(),
                Instant.EPOCH, BookingStatus.ACTIVE);
        repositories.slots().save(slot);
        repositories.bookings().save(booking);

        Booking completed = repositories.lifecycle().completeActiveBooking(tutorId, booking.id());

        assertEquals(BookingStatus.COMPLETED, completed.status());
        assertEquals(BookingStatus.COMPLETED,
                repositories.bookings().findById(booking.id()).orElseThrow().status());
        assertEquals(SlotStatus.COMPLETED,
                repositories.slots().findById(slot.id()).orElseThrow().status());
    }

    @Test
    void completeActiveBooking_missingBooking_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> repositories.lifecycle().completeActiveBooking(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void completeActiveBooking_missingSlot_rejectsRequest() {
        Booking booking = storeBooking(UUID.randomUUID(), BookingStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class,
                () -> repositories.lifecycle().completeActiveBooking(UUID.randomUUID(), booking.id()));
    }

    @Test
    void completeActiveBooking_otherTutorBooking_rejectsRequest() {
        ConsultationSlot slot = storeSlot(UUID.randomUUID(), SlotStatus.BOOKED);
        Booking booking = storeBooking(slot.id(), BookingStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class,
                () -> repositories.lifecycle().completeActiveBooking(UUID.randomUUID(), booking.id()));
    }

    @Test
    void completeActiveBooking_nonActiveBooking_rejectsRequest() {
        UUID tutorId = UUID.randomUUID();
        ConsultationSlot slot = storeSlot(tutorId, SlotStatus.BOOKED);
        Booking booking = storeBooking(slot.id(), BookingStatus.CANCELLED);

        assertThrows(IllegalArgumentException.class,
                () -> repositories.lifecycle().completeActiveBooking(tutorId, booking.id()));
    }

    @Test
    void completeActiveBooking_nonBookedSlot_rejectsRequest() {
        UUID tutorId = UUID.randomUUID();
        ConsultationSlot slot = storeSlot(tutorId, SlotStatus.AVAILABLE);
        Booking booking = storeBooking(slot.id(), BookingStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class,
                () -> repositories.lifecycle().completeActiveBooking(tutorId, booking.id()));
    }

    @RepeatedTest(10)
    void bookAvailableSlot_independentBundlesRace_commitsOneConsistentBooking() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-28T08:00:00Z");
        Path database = directory.resolve(UUID.randomUUID() + ".db");
        Repositories firstBundle = SqliteRepositories.open(database);
        Student student = new Student(studentId, "Alice", "alice@example.edu", true);
        Tutor tutor = new Tutor(tutorId, "Ada", "ada@example.edu", true);
        Module module = new Module(moduleId, "CS3227", "Software Engineering", true);
        ConsultationSlot slot = new ConsultationSlot(slotId, tutorId, moduleId,
                now.plusSeconds(3600), now.plusSeconds(5400), SlotStatus.AVAILABLE);
        firstBundle.users().save(student);
        firstBundle.users().save(tutor);
        firstBundle.modules().save(module);
        firstBundle.modules().assign(new TutorModule(tutorId, moduleId));
        firstBundle.slots().save(slot);
        Repositories secondBundle = SqliteRepositories.open(database);

        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> {
                start.await();
                try {
                    firstBundle.lifecycle().bookAvailableSlot(studentId, slotId, now);
                    return true;
                } catch (IllegalArgumentException rejected) {
                    return false;
                }
            });
            var second = pool.submit(() -> {
                start.await();
                try {
                    secondBundle.lifecycle().bookAvailableSlot(studentId, slotId, now);
                    return true;
                } catch (IllegalArgumentException rejected) {
                    return false;
                }
            });
            start.countDown();
            assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
        }

        assertEquals(1, firstBundle.bookings().findBySlotId(slotId).stream()
                .filter(booking -> booking.status() == BookingStatus.ACTIVE).count());
        assertEquals(SlotStatus.BOOKED, firstBundle.slots().findById(slotId).orElseThrow().status());
    }

    private ConsultationSlot storeSlot(UUID tutorId, SlotStatus status) {
        ConsultationSlot slot = new ConsultationSlot(UUID.randomUUID(), tutorId, UUID.randomUUID(),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1800), status);
        repositories.slots().save(slot);
        return slot;
    }

    private Booking storeBooking(UUID slotId, BookingStatus status) {
        Booking booking = new Booking(UUID.randomUUID(), UUID.randomUUID(), slotId, Instant.EPOCH, status);
        repositories.bookings().save(booking);
        return booking;
    }
}
