package model.consultation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Times are absolute instants; the UI chooses the display time zone. */
public record ConsultationSlot(UUID id, UUID tutorId, UUID moduleId,
                               Instant startTime, Instant endTime, SlotStatus status) {
    public ConsultationSlot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tutorId, "tutorId");
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
        Objects.requireNonNull(status, "status");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    public ConsultationSlot withStatus(SlotStatus next) {
        return new ConsultationSlot(id, tutorId, moduleId, startTime, endTime, next);
    }
}
