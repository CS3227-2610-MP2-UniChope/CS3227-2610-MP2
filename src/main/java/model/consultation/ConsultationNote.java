package model.consultation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import model.Validation;

/** One note per booking, edited by replacing the immutable value. */
public record ConsultationNote(UUID bookingId, String content, Instant updatedAt) {
    public ConsultationNote {
        Objects.requireNonNull(bookingId, "bookingId");
        content = Validation.text(content, "content");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
