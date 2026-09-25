package admin;

import java.util.UUID;
import model.consultation.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminQueriesTest {
    @Test
    void mixedStatusesAndRebookingsUseAllRecordsAsDenominator() {
        var f = new AdminFixture();
        var slot = f.slot(f.clock.instant(), SlotStatus.COMPLETED);
        f.booking(slot, BookingStatus.CANCELLED);
        f.booking(slot, BookingStatus.COMPLETED);
        f.booking(f.slot(f.clock.instant().plusSeconds(3600), SlotStatus.BOOKED), BookingStatus.ACTIVE);
        var result = AdminQueries.statistics(f.service.load());
        assertEquals(3, result.total());
        assertEquals(100.0 / 3, result.completionRate(), 0.0001);
        assertEquals(100.0 / 3, result.cancellationRate(), 0.0001);
        assertEquals(3, result.byModule().stream().mapToLong(AdminQueries.CountRow::count).sum());
        assertEquals(3, result.byTutor().getFirst().count());
    }

    @Test
    void emptyAndAllCompletedOrCancelledStatisticsAreDefined() {
        var f = new AdminFixture();
        assertEquals(0, AdminQueries.statistics(f.service.load()).completionRate());
        assertEquals(0, AdminQueries.statistics(f.service.load()).cancellationRate());
        var slot = f.slot(f.clock.instant(), SlotStatus.COMPLETED);
        var booking = f.booking(slot, BookingStatus.COMPLETED);
        assertEquals(100, AdminQueries.statistics(f.service.load()).completionRate());
        f.data.bookings().save(booking.withStatus(BookingStatus.CANCELLED));
        assertEquals(100, AdminQueries.statistics(f.service.load()).cancellationRate());
    }

    @Test
    void inactiveEntitiesRemainVisibleAndMissingReferencesAreNotDropped() {
        var f = new AdminFixture();
        var slot = f.slot(f.clock.instant().minusSeconds(3600), SlotStatus.COMPLETED);
        f.booking(slot, BookingStatus.COMPLETED);
        f.data.users().save(f.tutor.withActive(false));
        f.data.modules().save(f.module.withActive(false));
        f.data.bookings().save(new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                f.clock.instant(), BookingStatus.CANCELLED));
        var rows = AdminQueries.bookings(f.service.load());
        assertEquals(2, rows.size());
        assertTrue(rows.getFirst().tutor().contains("inactive"));
        assertTrue(rows.getFirst().module().contains("inactive"));
        assertEquals("Unavailable", rows.getLast().student());
        assertNull(rows.getLast().start());
        var result = AdminQueries.statistics(f.service.load());
        assertTrue(result.byModule().stream().anyMatch(r -> r.label().equals("Unknown") && r.count() == 1));
        assertEquals(2, result.byTutor().stream().mapToLong(AdminQueries.CountRow::count).sum());
    }

    @Test
    void aUserNamedUnavailableIsNotMistakenForAMissingReference() {
        var f = new AdminFixture();
        f.data.users().save(new model.user.Tutor(f.tutor.id(), "Unavailable", f.tutor.email(), true));
        f.booking(f.slot(f.clock.instant(), SlotStatus.COMPLETED), BookingStatus.COMPLETED);
        var stats = AdminQueries.statistics(f.service.load());
        assertEquals("Unavailable (tutor@example.edu)", stats.byTutor().getFirst().label());
    }

    @Test
    void sameNamedTutorsRemainSeparateAndBookingRowsAreChronological() {
        var f = new AdminFixture();
        var other = new model.user.Tutor(UUID.randomUUID(), f.tutor.name(), "other@example.edu", true);
        f.data.users().save(other);
        f.booking(f.slot(f.clock.instant().plusSeconds(3600), SlotStatus.COMPLETED), BookingStatus.COMPLETED);
        var first = new ConsultationSlot(UUID.randomUUID(), other.id(), f.module.id(),
                f.clock.instant(), f.clock.instant().plusSeconds(1800), SlotStatus.COMPLETED);
        f.data.slots().save(first);
        var booking = f.booking(first, BookingStatus.COMPLETED);
        var snapshot = f.service.load();
        assertEquals(booking.id(), AdminQueries.bookings(snapshot).getFirst().id());
        assertEquals(2, AdminQueries.statistics(snapshot).byTutor().size());
    }
}
