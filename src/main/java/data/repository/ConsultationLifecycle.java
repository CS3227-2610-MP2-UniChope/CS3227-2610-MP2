package data.repository;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import model.consultation.Booking;

/** Performs cross-repository consultation state transitions atomically. */
public interface ConsultationLifecycle {
    Booking completeActiveBooking(UUID tutorId, UUID bookingId);

    /** Atomically creates an active booking and marks its future slot booked. */
    default Booking bookAvailableSlot(UUID studentId, UUID slotId, Instant now) {
        throw new UnsupportedOperationException("Booking is not supported by this storage provider");
    }

    /** Atomically cancels a student's future booking and releases its slot. */
    default Booking cancelActiveBooking(UUID studentId, UUID bookingId, Instant now) {
        throw new UnsupportedOperationException("Cancellation is not supported by this storage provider");
    }

    /**
     * Serializes a read/check followed by at most one repository mutation against
     * other operations on this repository bundle. Callers must validate before
     * writing and must not perform external effects inside the callback.
     * Persistence implementations must provide an equivalent database transaction;
     * the default fails safely so existing implementations remain source compatible.
     * Slot/booking creation must also validate active entities in this boundary.
     */
    default <T> T withExclusiveAccess(Supplier<T> operation) {
        throw new UnsupportedOperationException("Guarded updates are not supported by this storage provider");
    }
}
