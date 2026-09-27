package student;

import java.time.LocalDate;
import java.util.Locale;

/** Optional search criteria for a student's booking history. */
public record BookingFilter(String course, String tutor, LocalDate date) {
    public BookingFilter {
        course = normalize(course);
        tutor = normalize(tutor);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
    }
}
