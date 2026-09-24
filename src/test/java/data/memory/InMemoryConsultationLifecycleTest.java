package data.memory;

import data.repository.Repositories;
import java.time.Instant;
import java.util.UUID;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryConsultationLifecycleTest {
    private final Repositories repositories = InMemoryRepositories.create();

    @Test
    void completesActiveBookingAndBookedSlotTogether() {
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
    void rejectsMissingBooking() {
        assertThrows(IllegalArgumentException.class,
                () -> repositories.lifecycle().completeActiveBooking(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void rejectsBookingWhoseSlotIsMissing() {
        Booking booking = storeBooking(UUID.randomUUID(), BookingStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class,
                () -> repositories.lifecycle().completeActiveBooking(UUID.randomUUID(), booking.id()));
    }

    @Test
    void rejectsBookingOwnedByAnotherTutor() {
        ConsultationSlot slot = storeSlot(UUID.randomUUID(), SlotStatus.BOOKED);
        Booking booking = storeBooking(slot.id(), BookingStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class,
                () -> repositories.lifecycle().completeActiveBooking(UUID.randomUUID(), booking.id()));
    }

    @Test
    void rejectsBookingThatIsNotActive() {
        UUID tutorId = UUID.randomUUID();
        ConsultationSlot slot = storeSlot(tutorId, SlotStatus.BOOKED);
        Booking booking = storeBooking(slot.id(), BookingStatus.CANCELLED);

        assertThrows(IllegalArgumentException.class,
                () -> repositories.lifecycle().completeActiveBooking(tutorId, booking.id()));
    }

    @Test
    void rejectsBookingWhoseSlotIsNotBooked() {
        UUID tutorId = UUID.randomUUID();
        ConsultationSlot slot = storeSlot(tutorId, SlotStatus.AVAILABLE);
        Booking booking = storeBooking(slot.id(), BookingStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class,
                () -> repositories.lifecycle().completeActiveBooking(tutorId, booking.id()));
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
