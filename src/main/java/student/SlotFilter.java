package student;

import java.time.LocalDate;
import java.util.Locale;

/** Optional search criteria for available consultation slots. */
public record SlotFilter(String module, String tutor, LocalDate date) {
    public SlotFilter {
        module = normalize(module);
        tutor = normalize(tutor);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
    }
}
