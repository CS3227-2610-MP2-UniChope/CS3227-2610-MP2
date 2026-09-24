package data.repository;

import java.util.UUID;
import java.util.function.Supplier;
import model.consultation.Booking;

/** Performs cross-repository consultation state transitions atomically. */
public interface ConsultationLifecycle {
    Booking completeActiveBooking(UUID tutorId, UUID bookingId);

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
