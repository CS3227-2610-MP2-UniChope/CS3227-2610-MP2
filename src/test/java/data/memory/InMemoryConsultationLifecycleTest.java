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
