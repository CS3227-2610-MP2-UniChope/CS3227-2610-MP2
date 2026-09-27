package student;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SlotTimelineTest {
    @Test void overlappingChoicesHaveSeparateLanesButAdjacentSlotsShareOne() {
        var first = slot("CS1", "Tutor", "09:00", "10:00");
        var overlap = slot("CS1", "Tutor", "09:30", "10:30");
        var adjacent = slot("CS1", "Tutor", "10:00", "11:00");
        var otherTutor = slot("CS1", "Other tutor", "09:00", "10:00");
        var otherCourse = slot("CS2", "Tutor", "09:00", "10:00");
        var lanes = SlotTimeline.lanes(List.of(overlap, adjacent, otherTutor, first, otherCourse));
        assertEquals(4, lanes.size());
        assertTrue(lanes.contains(List.of(first, adjacent)));
        assertTrue(lanes.contains(List.of(overlap)));
        assertEquals(5, lanes.stream().mapToInt(List::size).sum());
    }

    private static StudentService.SlotRow slot(String course, String tutor, String start, String end) {
        return new StudentService.SlotRow(UUID.randomUUID(), course, tutor,
                Instant.parse("2026-09-26T" + start + ":00Z"), Instant.parse("2026-09-26T" + end + ":00Z"));
    }
}
