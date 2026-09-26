package tutor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import model.consultation.ConsultationSlot;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.SlotStatus;
import model.module.TutorModule;
import model.module.Module;
import model.user.Tutor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TutorServiceTest {
    @Test
    void createSlot_activeAssignedTutor_createsAvailableSlot() {
        var f = new TutorFixture();

        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));

        assertEquals(SlotStatus.AVAILABLE, slot.status());
        assertEquals(slot, f.data.slots().findById(slot.id()).orElseThrow());
    }

    @Test
    void createSlot_missingTutor_rejectsRequest() {
        var f = new TutorFixture();
        assertThrows(SecurityException.class, () -> f.service(UUID.randomUUID()).createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400)));
    }

    @Test
    void createSlot_inactiveTutor_rejectsRequest() {
        var f = new TutorFixture();
        f.data.users().save(f.tutor.withActive(false));

        assertThrows(SecurityException.class, () -> f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400)));
    }

    @Test
    void createSlot_inactiveModule_rejectsRequest() {
        var f = new TutorFixture();
        f.data.modules().save(f.module.withActive(false));

        assertThrows(IllegalArgumentException.class, () -> f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400)));
    }

    @Test
    void createSlot_unassignedTutor_rejectsRequest() {
        var f = new TutorFixture();
        f.data.modules().unassign(new TutorModule(f.tutor.id(), f.module.id()));

        assertThrows(IllegalArgumentException.class, () -> f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400)));
    }

    @Test
    void createSlot_pastStartTime_rejectsRequest() {
        var f = new TutorFixture();

        assertThrows(IllegalArgumentException.class, () -> f.service.createSlot(f.module.id(),
                f.now.minusSeconds(1), f.now.plusSeconds(1800)));
    }

    @Test
    void createSlot_overlappingAvailableSlot_rejectsRequest() {
        var f = new TutorFixture();
        f.service.createSlot(f.module.id(), f.now.plusSeconds(3600), f.now.plusSeconds(5400));

        assertThrows(IllegalArgumentException.class, () -> f.service.createSlot(f.module.id(),
                f.now.plusSeconds(4500), f.now.plusSeconds(6300)));
    }

    @Test
    void createSlot_overlappingBookedSlot_rejectsRequest() {
        var f = new TutorFixture();
        ConsultationSlot existing = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(existing.withStatus(SlotStatus.BOOKED));

        assertThrows(IllegalArgumentException.class, () -> f.service.createSlot(f.module.id(),
                f.now.plusSeconds(4500), f.now.plusSeconds(6300)));
    }

    @Test
    void createSlot_adjacentSlot_createsAvailableSlot() {
        var f = new TutorFixture();
        f.service.createSlot(f.module.id(), f.now.plusSeconds(3600), f.now.plusSeconds(5400));

        ConsultationSlot adjacent = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(5400), f.now.plusSeconds(7200));

        assertEquals(SlotStatus.AVAILABLE, adjacent.status());
    }

    @Test
    void createSlot_overlappingCancelledSlot_createsAvailableSlot() {
        var f = new TutorFixture();
        ConsultationSlot existing = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(existing.withStatus(SlotStatus.CANCELLED));

        ConsultationSlot replacement = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(4500), f.now.plusSeconds(6300));

        assertEquals(SlotStatus.AVAILABLE, replacement.status());
    }

    @Test
    void createSlot_overlappingCompletedSlot_createsAvailableSlot() {
        var f = new TutorFixture();
        ConsultationSlot existing = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(existing.withStatus(SlotStatus.COMPLETED));

        ConsultationSlot replacement = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(4500), f.now.plusSeconds(6300));

        assertEquals(SlotStatus.AVAILABLE, replacement.status());
    }

    @Test
    void createSlot_concurrentOverlap_acceptsOnlyOneRequest() throws Exception {
        var f = new TutorFixture();
        Callable<Boolean> attempt = () -> {
            try {
                f.service.createSlot(f.module.id(), f.now.plusSeconds(3600), f.now.plusSeconds(5400));
                return true;
            } catch (IllegalArgumentException conflict) {
                return false;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(java.util.List.of(attempt, attempt));
            long accepted = results.stream().filter(result -> {
                try { return result.get(); } catch (Exception failure) { throw new AssertionError(failure); }
            }).count();
            assertEquals(1, accepted);
        }
        assertEquals(1, f.data.slots().findByTutorId(f.tutor.id()).size());
    }

    @Test
    void cancelSlot_ownedAvailableSlot_marksSlotCancelled() {
        var f = new TutorFixture();
        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));

        ConsultationSlot cancelled = f.service.cancelSlot(slot.id());

        assertEquals(SlotStatus.CANCELLED, cancelled.status());
        assertEquals(SlotStatus.CANCELLED, f.data.slots().findById(slot.id()).orElseThrow().status());
    }

    @Test
    void cancelSlot_otherTutorsSlot_rejectsRequest() {
        var f = new TutorFixture();
        Tutor otherTutor = new Tutor(UUID.randomUUID(), "Grace", "grace@example.edu", true);
        f.data.users().save(otherTutor);
        f.data.modules().assign(new TutorModule(otherTutor.id(), f.module.id()));
        ConsultationSlot slot = f.service(otherTutor.id()).createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));

        assertThrows(IllegalArgumentException.class, () -> f.service.cancelSlot(slot.id()));
    }

    @Test
    void cancelSlot_bookedSlot_rejectsRequest() {
        var f = new TutorFixture();
        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(slot.withStatus(SlotStatus.BOOKED));

        assertThrows(IllegalArgumentException.class, () -> f.service.cancelSlot(slot.id()));
    }

    @Test
    void cancelSlot_completedSlot_rejectsRequest() {
        var f = new TutorFixture();
        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(slot.withStatus(SlotStatus.COMPLETED));

        assertThrows(IllegalArgumentException.class, () -> f.service.cancelSlot(slot.id()));
    }

    @Test
    void cancelSlot_missingSlot_rejectsRequest() {
        var f = new TutorFixture();

        assertThrows(IllegalArgumentException.class, () -> f.service.cancelSlot(UUID.randomUUID()));
    }

    @Test
    void cancelSlot_cancelledSlot_rejectsRequest() {
        var f = new TutorFixture();
        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(slot.withStatus(SlotStatus.CANCELLED));

        assertThrows(IllegalArgumentException.class, () -> f.service.cancelSlot(slot.id()));
    }

    @Test
    void findUpcomingSlots_matchingSingaporeDate_returnsSortedAvailableAndBookedSlots() {
        var f = new TutorFixture();
        ConsultationSlot later = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(7200), f.now.plusSeconds(9000));
        ConsultationSlot earlier = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(later.withStatus(SlotStatus.BOOKED));
        ConsultationSlot cancelled = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(10800), f.now.plusSeconds(12600));
        f.data.slots().save(cancelled.withStatus(SlotStatus.CANCELLED));

        var result = f.service.findUpcomingSlots(LocalDate.of(2026, 9, 24));

        assertEquals(java.util.List.of(earlier, later.withStatus(SlotStatus.BOOKED)), result);
    }

    @Test
    void findUpcomingSlots_slotAfterUtcMidnightBoundary_matchesSingaporeDate() {
        var f = new TutorFixture();
        Instant start = Instant.parse("2026-09-24T16:30:00Z");
        ConsultationSlot slot = f.service.createSlot(f.module.id(), start, start.plusSeconds(1800));

        var result = f.service.findUpcomingSlots(LocalDate.of(2026, 9, 25));

        assertEquals(java.util.List.of(slot), result);
    }

    @Test
    void findUpcomingSlots_pastAvailableSlot_excludesSlot() {
        var f = new TutorFixture();
        ConsultationSlot past = new ConsultationSlot(UUID.randomUUID(), f.tutor.id(), f.module.id(),
                f.now.minusSeconds(3600), f.now.minusSeconds(1800), SlotStatus.AVAILABLE);
        f.data.slots().save(past);

        var result = f.service.findUpcomingSlots(LocalDate.of(2026, 9, 24));

        assertEquals(java.util.List.of(), result);
    }

    @Test
    void findBookings_matchingStatus_returnsOnlyTutorsBookings() {
        var f = new TutorFixture();
        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(slot.withStatus(SlotStatus.BOOKED));
        Booking active = new Booking(UUID.randomUUID(), UUID.randomUUID(), slot.id(), f.now, BookingStatus.ACTIVE);
        f.data.bookings().save(active);
        Booking cancelled = new Booking(UUID.randomUUID(), UUID.randomUUID(), slot.id(),
                f.now.plusSeconds(1), BookingStatus.CANCELLED);
        f.data.bookings().save(cancelled);

        var result = f.service.findBookings(new BookingFilter(null, null, BookingStatus.ACTIVE));

        assertEquals(java.util.List.of(active), result);
    }

    @Test
    void findBookings_matchingModule_returnsOnlyMatchingBookings() {
        var f = new TutorFixture();
        ConsultationSlot firstSlot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(firstSlot.withStatus(SlotStatus.BOOKED));
        Booking first = new Booking(UUID.randomUUID(), UUID.randomUUID(), firstSlot.id(), f.now, BookingStatus.ACTIVE);
        f.data.bookings().save(first);
        Module otherModule = new Module(UUID.randomUUID(), "CS2103", "Software Engineering", true);
        f.data.modules().save(otherModule);
        f.data.modules().assign(new TutorModule(f.tutor.id(), otherModule.id()));
        ConsultationSlot secondSlot = f.service.createSlot(otherModule.id(),
                f.now.plusSeconds(7200), f.now.plusSeconds(9000));
        f.data.slots().save(secondSlot.withStatus(SlotStatus.BOOKED));
        f.data.bookings().save(new Booking(UUID.randomUUID(), UUID.randomUUID(), secondSlot.id(),
                f.now, BookingStatus.ACTIVE));

        var result = f.service.findBookings(new BookingFilter(f.module.id(), null, null));

        assertEquals(java.util.List.of(first), result);
    }

    @Test
    void findBookings_matchingSingaporeDate_returnsOnlyBookingsForThatDate() {
        var f = new TutorFixture();
        ConsultationSlot firstSlot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(firstSlot.withStatus(SlotStatus.BOOKED));
        f.data.bookings().save(new Booking(UUID.randomUUID(), UUID.randomUUID(), firstSlot.id(),
                f.now, BookingStatus.ACTIVE));
        Instant nextSingaporeDate = Instant.parse("2026-09-24T16:30:00Z");
        ConsultationSlot secondSlot = f.service.createSlot(f.module.id(),
                nextSingaporeDate, nextSingaporeDate.plusSeconds(1800));
        f.data.slots().save(secondSlot.withStatus(SlotStatus.BOOKED));
        Booking second = new Booking(UUID.randomUUID(), UUID.randomUUID(), secondSlot.id(),
                f.now, BookingStatus.ACTIVE);
        f.data.bookings().save(second);

        var result = f.service.findBookings(new BookingFilter(null, LocalDate.of(2026, 9, 25), null));

        assertEquals(java.util.List.of(second), result);
    }

    @Test
    void findBookings_unsortedStorage_returnsBookingsBySlotStartTime() {
        var f = new TutorFixture();
        ConsultationSlot laterSlot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(7200), f.now.plusSeconds(9000));
        f.data.slots().save(laterSlot.withStatus(SlotStatus.BOOKED));
        Booking later = new Booking(UUID.randomUUID(), UUID.randomUUID(), laterSlot.id(), f.now, BookingStatus.ACTIVE);
        f.data.bookings().save(later);
        ConsultationSlot earlierSlot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(earlierSlot.withStatus(SlotStatus.BOOKED));
        Booking earlier = new Booking(UUID.randomUUID(), UUID.randomUUID(), earlierSlot.id(), f.now, BookingStatus.ACTIVE);
        f.data.bookings().save(earlier);

        var result = f.service.findBookings(new BookingFilter(null, null, null));

        assertEquals(java.util.List.of(earlier, later), result);
    }

    @Test
    void findBookings_otherTutorsBooking_excludesBooking() {
        var f = new TutorFixture();
        Tutor otherTutor = new Tutor(UUID.randomUUID(), "Grace", "grace@example.edu", true);
        f.data.users().save(otherTutor);
        f.data.modules().assign(new TutorModule(otherTutor.id(), f.module.id()));
        ConsultationSlot slot = f.service(otherTutor.id()).createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(slot.withStatus(SlotStatus.BOOKED));
        f.data.bookings().save(new Booking(UUID.randomUUID(), UUID.randomUUID(), slot.id(),
                f.now, BookingStatus.ACTIVE));

        var result = f.service.findBookings(new BookingFilter(null, null, BookingStatus.ACTIVE));

        assertEquals(java.util.List.of(), result);
    }

    @Test
    void findBookings_inactiveTutor_rejectsRequest() {
        var f = new TutorFixture();
        f.data.users().save(f.tutor.withActive(false));

        assertThrows(SecurityException.class,
                () -> f.service.findBookings(new BookingFilter(null, null, null)));
    }
}
