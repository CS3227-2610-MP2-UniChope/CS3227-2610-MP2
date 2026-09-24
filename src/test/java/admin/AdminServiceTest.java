package admin;

import data.repository.Repositories;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import model.consultation.*;
import model.module.Module;
import model.user.*;
import org.junit.jupiter.api.Test;
import util.OperationLog;
import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest {
    private final AdminFixture f = new AdminFixture();

    @Test
    void authorizationIsRecheckedForEveryReadAndMutation() {
        for (UUID actor : List.of(f.student.id(), f.tutor.id(), UUID.randomUUID())) {
            var service = new AdminService(f.data, actor, f.clock, new OperationLog(f.clock, e -> { }));
            assertThrows(SecurityException.class, service::load);
            assertThrows(SecurityException.class, () -> service.createModule("X", "Unauthorized"));
        }
        f.data.users().save(f.actor.withActive(false));
        assertThrows(SecurityException.class, f.service::load);
        assertThrows(SecurityException.class, () -> f.service.addUser(Role.STUDENT, "No", "no@example.edu"));
        assertEquals(1, f.data.modules().findAll().size());
        assertEquals(3, f.data.users().findAll().size());
    }

    @Test
    void createsOnlyStudentsAndTutorsAndRejectsInvalidOrDuplicateInputs() {
        User user = f.service.addUser(Role.TUTOR, " New Tutor ", "new@example.edu");
        assertEquals(Role.TUTOR, user.role());
        assertEquals("New Tutor", user.name());
        assertThrows(IllegalArgumentException.class, () -> f.service.addUser(Role.ADMIN, "X", "x@example.edu"));
        assertThrows(IllegalArgumentException.class, () -> f.service.addUser(Role.STUDENT, " ", "x@example.edu"));
        assertThrows(IllegalArgumentException.class, () -> f.service.addUser(Role.STUDENT, "X", "bad-email"));
        f.service.deactivateUser(user.id());
        assertThrows(IllegalArgumentException.class, () -> f.service.addUser(Role.STUDENT, "X", "NEW@example.edu"));
        assertEquals(4, f.data.users().findAll().size());
    }

    @Test
    void historicalBookingsSurviveDeactivationAndRepeatedDeactivation() {
        var slot = f.slot(f.clock.instant().minusSeconds(3600), SlotStatus.COMPLETED);
        var booking = f.booking(slot, BookingStatus.COMPLETED);
        f.service.deactivateUser(f.student.id());
        f.service.deactivateUser(f.student.id());
        f.service.deactivateUser(f.tutor.id());
        f.service.deactivateModule(f.module.id());
        assertEquals(booking, f.data.bookings().findById(booking.id()).orElseThrow());
        assertEquals(slot, f.data.slots().findById(slot.id()).orElseThrow());
        assertFalse(f.data.users().findById(f.student.id()).orElseThrow().isActive());
        assertThrows(IllegalArgumentException.class, () -> f.service.deactivateUser(f.actor.id()));
        assertThrows(IllegalArgumentException.class, () -> f.service.deactivateUser(UUID.randomUUID()));
    }

    @Test
    void overdueActiveBookingsBlockAllAffectedDeactivationsAndUnassignment() {
        f.service.assign(f.tutor.id(), f.module.id());
        var slot = f.slot(f.clock.instant().minusSeconds(3600), SlotStatus.BOOKED);
        f.booking(slot, BookingStatus.ACTIVE);
        assertThrows(IllegalArgumentException.class, () -> f.service.deactivateUser(f.student.id()));
        assertThrows(IllegalArgumentException.class, () -> f.service.deactivateUser(f.tutor.id()));
        assertThrows(IllegalArgumentException.class, () -> f.service.deactivateModule(f.module.id()));
        assertThrows(IllegalArgumentException.class, () -> f.service.unassign(f.tutor.id(), f.module.id()));
        assertTrue(f.data.users().findById(f.student.id()).orElseThrow().isActive());
        assertTrue(f.data.modules().findById(f.module.id()).orElseThrow().isActive());
        assertEquals(1, f.data.modules().findAssignments().size());
    }

    @Test
    void futureAvailableSlotsBlockTutorModuleAndPairButNotUnrelatedRecords() {
        f.service.assign(f.tutor.id(), f.module.id());
        f.slot(f.clock.instant().plusSeconds(60), SlotStatus.AVAILABLE);
        assertThrows(IllegalArgumentException.class, () -> f.service.deactivateUser(f.tutor.id()));
        assertThrows(IllegalArgumentException.class, () -> f.service.deactivateModule(f.module.id()));
        assertThrows(IllegalArgumentException.class, () -> f.service.unassign(f.tutor.id(), f.module.id()));
        Module other = f.service.createModule("CS2103", "Other");
        f.service.assign(f.tutor.id(), other.id());
        assertTrue(f.service.unassign(f.tutor.id(), other.id()));
        assertFalse(f.service.unassign(f.tutor.id(), other.id()));
        assertFalse(f.service.deactivateUser(f.student.id()).isActive());
    }

    @Test
    void pastAvailableAndCancelledFutureSlotsDoNotBlock() {
        f.slot(f.clock.instant().minusSeconds(3600), SlotStatus.AVAILABLE);
        f.slot(f.clock.instant().plusSeconds(3600), SlotStatus.CANCELLED);
        f.service.assign(f.tutor.id(), f.module.id());
        assertTrue(f.service.unassign(f.tutor.id(), f.module.id()));
        assertFalse(f.service.deactivateModule(f.module.id()).isActive());
    }

    @Test
    void moduleEditingPreservesIdentityAndStatusAndRejectsDuplicates() {
        Module added = f.service.createModule("CS2103", "Original");
        var assignment = f.service.assign(f.tutor.id(), added.id());
        var edited = f.service.editModule(added.id(), "CS2103T", "Changed");
        assertEquals(added.id(), edited.id());
        assertEquals(List.of(assignment), f.data.modules().findAssignments());
        f.service.deactivateModule(edited.id());
        assertFalse(f.service.editModule(edited.id(), "CS2103T", "Still inactive").isActive());
        assertThrows(IllegalArgumentException.class, () -> f.service.createModule("cs2103t", "Duplicate"));
        assertThrows(IllegalArgumentException.class, () -> f.service.editModule(f.module.id(), "CS2103T", "Dup"));
        assertThrows(IllegalArgumentException.class, () -> f.service.createModule(" ", "Invalid"));
        assertThrows(IllegalArgumentException.class, () -> f.service.createModule("CODE", " "));
        assertThrows(IllegalArgumentException.class, () -> f.service.editModule(UUID.randomUUID(), "X", "Missing"));
        assertEquals("CS3227", f.data.modules().findById(f.module.id()).orElseThrow().code());
    }

    @Test
    void assignmentsRequireActiveTutorAndModuleAndAreIdempotent() {
        var assignment = f.service.assign(f.tutor.id(), f.module.id());
        assertEquals(assignment, f.service.assign(f.tutor.id(), f.module.id()));
        assertEquals(1, f.data.modules().findAssignments().size());
        assertThrows(IllegalArgumentException.class, () -> f.service.assign(f.student.id(), f.module.id()));
        assertThrows(IllegalArgumentException.class, () -> f.service.assign(UUID.randomUUID(), f.module.id()));
        assertThrows(IllegalArgumentException.class, () -> f.service.assign(f.tutor.id(), UUID.randomUUID()));
        f.service.deactivateUser(f.tutor.id());
        assertThrows(IllegalArgumentException.class, () -> f.service.assign(f.tutor.id(), f.module.id()));
        f.data.users().save(f.tutor);
        f.service.deactivateModule(f.module.id());
        assertThrows(IllegalArgumentException.class, () -> f.service.assign(f.tutor.id(), f.module.id()));
    }

    @Test
    void failingOrUnsupportedStorageDoesNotReportSuccessOrMutate() {
        var unsupported = new Repositories(f.data.users(), f.data.modules(), f.data.slots(),
                f.data.bookings(), (tutor, booking) -> { throw new UnsupportedOperationException(); });
        var events = new java.util.ArrayList<OperationLog.Event>();
        var service = new AdminService(unsupported, f.actor.id(), f.clock, new OperationLog(f.clock, events::add));
        assertThrows(UnsupportedOperationException.class, () -> service.createModule("X", "Test"));
        assertEquals(1, f.data.modules().findAll().size());
        assertEquals(OperationLog.Outcome.FAILURE, events.getFirst().outcome());
    }

    @Test
    void repositoryWriteFailureLeavesDataUnchangedAndProducesFailureDiagnostic() {
        var failingUsers = new data.repository.UserRepository() {
            @Override public User save(User value) { throw new IllegalStateException("Database unavailable"); }
            @Override public java.util.Optional<User> findById(UUID id) { return f.data.users().findById(id); }
            @Override public java.util.Optional<User> findByEmail(String email) { return f.data.users().findByEmail(email); }
            @Override public List<User> findAll() { return f.data.users().findAll(); }
        };
        var data = new Repositories(failingUsers, f.data.modules(), f.data.slots(), f.data.bookings(), f.data.lifecycle());
        var service = new AdminService(data, f.actor.id(), f.clock, new OperationLog(f.clock, f.events::add));
        assertThrows(IllegalStateException.class, () -> service.addUser(Role.STUDENT, "New", "new@example.edu"));
        assertEquals(3, f.data.users().findAll().size());
        assertEquals(OperationLog.Outcome.FAILURE, f.events.getLast().outcome());
    }

    @Test
    void loggingFailureDoesNotUndoOrMisreportSuccessfulWrite() {
        var log = new OperationLog(f.clock, event -> { throw new IllegalStateException("Unavailable"); });
        var service = new AdminService(f.data, f.actor.id(), f.clock, log);
        Module created = service.createModule("CS9999", "Test");
        assertTrue(f.data.modules().findById(created.id()).isPresent());
        assertEquals(1, log.metrics().deliveryFailures());
    }

    @Test
    void sharedGuardSerializesSlotCreationAgainstDeactivation() throws Exception {
        CountDownLatch guardHeld = new CountDownLatch(1);
        CountDownLatch releaseGuard = new CountDownLatch(1);
        CountDownLatch deactivateStarted = new CountDownLatch(1);
        try (var threads = Executors.newFixedThreadPool(2)) {
            var creation = threads.submit(() -> f.data.lifecycle().withExclusiveAccess(() -> {
                guardHeld.countDown();
                try {
                    if (!releaseGuard.await(5, TimeUnit.SECONDS)) { throw new AssertionError("Timed out"); }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                assertTrue(f.data.users().findById(f.tutor.id()).orElseThrow().isActive());
                return f.slot(f.clock.instant().plusSeconds(3600), SlotStatus.AVAILABLE);
            }));
            assertTrue(guardHeld.await(5, TimeUnit.SECONDS));
            var deactivation = threads.submit(() -> {
                deactivateStarted.countDown();
                assertThrows(IllegalArgumentException.class, () -> f.service.deactivateUser(f.tutor.id()));
            });
            try {
                assertTrue(deactivateStarted.await(5, TimeUnit.SECONDS));
            } finally { releaseGuard.countDown(); }
            creation.get(5, TimeUnit.SECONDS);
            deactivation.get(5, TimeUnit.SECONDS);
        }
        assertTrue(f.data.users().findById(f.tutor.id()).orElseThrow().isActive());
    }

    @Test
    void creationProtocolRejectsAlreadyDeactivatedTutor() {
        f.service.deactivateUser(f.tutor.id());
        assertThrows(IllegalArgumentException.class, () -> f.data.lifecycle().withExclusiveAccess(() -> {
            if (!f.data.users().findById(f.tutor.id()).orElseThrow().isActive()) {
                throw new IllegalArgumentException("Tutor inactive");
            }
            return f.slot(f.clock.instant().plusSeconds(60), SlotStatus.AVAILABLE);
        }));
        assertTrue(f.data.slots().findAll().isEmpty());
    }
}
