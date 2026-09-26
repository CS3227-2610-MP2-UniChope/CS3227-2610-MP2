package tutor;

import java.time.LocalDate;
import java.util.UUID;
import model.consultation.BookingStatus;

/** Optional criteria for a tutor's booking list. */
public record BookingFilter(UUID moduleId, LocalDate date, BookingStatus status) { }
