package model.consultation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Booking(UUID id, UUID studentId, UUID slotId,
                      Instant createdAt, BookingStatus status) {
    public Booking {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(slotId, "slotId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(status, "status");
    }

    public Booking withStatus(BookingStatus next) {
        return new Booking(id, studentId, slotId, createdAt, next);
    }
}
