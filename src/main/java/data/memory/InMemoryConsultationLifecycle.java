package data.memory;

import data.repository.ConsultationLifecycle;
import java.util.Objects;
import java.util.UUID;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;

final class InMemoryConsultationLifecycle implements ConsultationLifecycle {
    private final InMemorySlotRepository slots;
    private final InMemoryBookingRepository bookings;
    private final Object lock;

    InMemoryConsultationLifecycle(InMemorySlotRepository slots, InMemoryBookingRepository bookings,
                                  Object lock) {
        this.slots = Objects.requireNonNull(slots, "slots");
        this.bookings = Objects.requireNonNull(bookings, "bookings");
        this.lock = Objects.requireNonNull(lock, "lock");
    }

    @Override
    public Booking completeActiveBooking(UUID tutorId, UUID bookingId) {
        synchronized (lock) {
            Objects.requireNonNull(tutorId, "tutorId");
            Booking booking = bookings.findById(Objects.requireNonNull(bookingId, "bookingId"))
                    .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
            ConsultationSlot slot = slots.findById(booking.slotId())
                    .orElseThrow(() -> new IllegalArgumentException("Slot does not exist"));
            if (!slot.tutorId().equals(tutorId)) {
                throw new IllegalArgumentException("Booking does not belong to tutor");
            }
            if (booking.status() != BookingStatus.ACTIVE || slot.status() != SlotStatus.BOOKED) {
                throw new IllegalArgumentException("Booking and slot are not ready for completion");
            }
            Booking completedBooking = bookings.save(booking.withStatus(BookingStatus.COMPLETED));
            slots.save(slot.withStatus(SlotStatus.COMPLETED));
            return completedBooking;
        }
    }
}
