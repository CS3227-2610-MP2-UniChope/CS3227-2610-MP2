package util;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OperationLogTest {
    @Test
    void recordsStructuredEventsAndCountsWithoutExceptionMessageOrUserInput() {
        var events = new ArrayList<OperationLog.Event>();
        var clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        var log = new OperationLog(clock, events::add);
        UUID actor = UUID.randomUUID();
        UUID entity = UUID.randomUUID();
        log.record("admin.user.add", actor, entity, null);
        log.record("admin.user.add", actor, entity, new IllegalArgumentException("private@example.edu"));
        assertEquals(Instant.EPOCH, events.getFirst().time());
        assertEquals(actor, events.getFirst().actorId());
        assertEquals(entity, events.getFirst().entityId());
        assertEquals(OperationLog.Outcome.SUCCESS, events.getFirst().outcome());
        assertEquals("IllegalArgumentException", events.getLast().errorType());
        assertFalse(events.toString().contains("private@example.edu"));
        assertEquals(new OperationLog.Metrics(1, 1, 0), log.metrics());
        assertThrows(IllegalArgumentException.class, () -> log.record("private@example.edu", actor, entity, null));
    }

    @Test
    void failedSinkIsCountedAndDoesNotThrow() {
        var log = new OperationLog(Clock.systemUTC(), event -> { throw new IllegalStateException("offline"); });
        assertDoesNotThrow(() -> log.record("test.write", null, null, null));
        assertEquals(1, log.metrics().deliveryFailures());
        assertSame(OperationLog.application(), OperationLog.application());
    }
}
