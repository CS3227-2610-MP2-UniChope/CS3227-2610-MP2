package admin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import student.StudentService;
import tutor.TutorService;
import util.OperationLog;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the shared diagnostics contract through real role services and SQLite. */
class RoleOperationLogTest {
    @TempDir Path directory;

    @Test
    void allRolesReportOutcomesToSharedCountersWithoutPersonalData() {
        var f = new AdminFixture(directory.resolve("role-log.db"));
        var events = new ArrayList<OperationLog.Event>();
        var log = new OperationLog(f.clock, events::add);
        var admin = new AdminService(f.data, f.actor.id(), f.clock, log);
        var tutor = new TutorService(f.data, f.tutor.id(), f.clock, log);
        var student = new StudentService(f.data, f.student.id(), f.clock, log);

        admin.assign(f.tutor.id(), f.module.id());
        var start = f.clock.instant().plusSeconds(3600);
        var slot = tutor.createSlot(f.module.id(), start, start.plusSeconds(1800));
        var booking = student.book(slot.id());
        assertThrows(IllegalArgumentException.class, () -> admin.deactivateUser(f.student.id()));
        assertThrows(IllegalArgumentException.class, () -> tutor.cancelSlot(slot.id()));
        assertThrows(IllegalArgumentException.class, () -> student.book(slot.id()));
        student.cancel(booking.id());

        assertEquals(new OperationLog.Metrics(4, 3, 0), log.metrics());
        assertEquals(List.of("admin.assignment.add", "tutor.slot.create", "student.booking.create",
                "admin.user.deactivate", "tutor.slot.cancel", "student.booking.create",
                "student.booking.cancel"), events.stream().map(OperationLog.Event::operation).toList());
        assertEquals(f.actor.id(), events.get(0).actorId());
        assertEquals(f.tutor.id(), events.get(1).actorId());
        assertEquals(f.student.id(), events.get(2).actorId());
        assertEquals(OperationLog.Outcome.FAILURE, events.get(3).outcome());
        assertFalse(events.toString().contains("@example.edu"));
        assertFalse(events.toString().contains("Resolve this student's"));
        assertEquals(model.consultation.BookingStatus.CANCELLED,
                f.data.bookings().findById(booking.id()).orElseThrow().status());
    }
}
