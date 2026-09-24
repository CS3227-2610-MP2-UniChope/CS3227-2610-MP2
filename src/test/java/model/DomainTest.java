package model;

import java.time.Instant;
import java.util.UUID;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.user.Student;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainTest {
    @Test
    void invalidIdentityAndContactDetailsAreRejected() {
        assertThrows(NullPointerException.class, () -> new Student(null, "A", "a@example.edu", true));
        assertThrows(IllegalArgumentException.class, () -> new Student(UUID.randomUUID(), " ", "a@example.edu", true));
        assertThrows(IllegalArgumentException.class, () -> new Student(UUID.randomUUID(), "A", "invalid", true));
    }

    @Test
    void deactivationPreservesIdentityAndOriginalValue() {
        Student original = new Student(UUID.randomUUID(), " Alice ", " alice@example.edu ", true);
        Student inactive = original.withActive(false);
        assertTrue(original.isActive());
        assertFalse(inactive.isActive());
        assertEquals(original.id(), inactive.id());
        assertEquals("Alice", inactive.name());
        assertEquals("alice@example.edu", inactive.email());
    }

    @Test
    void slotsRequirePositiveDurationButAllowHistoricalTimes() {
        Instant start = Instant.parse("2026-01-01T01:00:00Z");
        UUID tutor = UUID.randomUUID();
        UUID module = UUID.randomUUID();
        for (Instant invalidEnd : new Instant[] {start, start.minusSeconds(1)}) {
            assertThrows(IllegalArgumentException.class, () -> new ConsultationSlot(
                    UUID.randomUUID(), tutor, module, start, invalidEnd, SlotStatus.AVAILABLE));
        }
        assertDoesNotThrow(() -> new ConsultationSlot(UUID.randomUUID(), tutor, module,
                start, start.plusSeconds(1800), SlotStatus.COMPLETED));
    }
}
