package data.repository;

import java.util.UUID;
import model.consultation.Booking;

/** Performs cross-repository consultation state transitions atomically. */
public interface ConsultationLifecycle {
    Booking completeActiveBooking(UUID tutorId, UUID bookingId);
}
