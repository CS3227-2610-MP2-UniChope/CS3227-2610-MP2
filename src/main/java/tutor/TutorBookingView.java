package tutor;

import java.time.Instant;
import java.util.UUID;
import model.consultation.BookingStatus;

/** Read-only booking information displayed in the tutor workspace. */
public record TutorBookingView(UUID bookingId, UUID slotId, String studentName, String moduleCode,
                               Instant startTime, Instant endTime, BookingStatus status) { }
