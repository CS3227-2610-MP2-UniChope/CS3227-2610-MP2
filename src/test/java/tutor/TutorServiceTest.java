package tutor;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.module.TutorModule;
import model.user.Tutor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TutorServiceTest {
    @Test
    void createSlot_activeAssignedTutor_createsAvailableSlot() {
        var f = new TutorFixture();

        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));

        assertEquals(SlotStatus.AVAILABLE, slot.status());
        assertEquals(slot, f.data.slots().findById(slot.id()).orElseThrow());
    }

    @Test
    void createSlot_missingTutor_rejectsRequest() {
        var f = new TutorFixture();
        assertThrows(SecurityException.class, () -> f.service(UUID.randomUUID()).createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400)));
    }

    @Test
    void createSlot_inactiveTutor_rejectsRequest() {
        var f = new TutorFixture();
        f.data.users().save(f.tutor.withActive(false));

        assertThrows(SecurityException.class, () -> f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400)));
    }

    @Test
    void createSlot_inactiveModule_rejectsRequest() {
        var f = new TutorFixture();
        f.data.modules().save(f.module.withActive(false));

        assertThrows(IllegalArgumentException.class, () -> f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400)));
    }

    @Test
    void createSlot_unassignedTutor_rejectsRequest() {
        var f = new TutorFixture();
        f.data.modules().unassign(new TutorModule(f.tutor.id(), f.module.id()));

        assertThrows(IllegalArgumentException.class, () -> f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400)));
    }

    @Test
    void createSlot_pastStartTime_rejectsRequest() {
        var f = new TutorFixture();

        assertThrows(IllegalArgumentException.class, () -> f.service.createSlot(f.module.id(),
                f.now.minusSeconds(1), f.now.plusSeconds(1800)));
    }

    @Test
    void createSlot_overlappingAvailableSlot_rejectsRequest() {
        var f = new TutorFixture();
        f.service.createSlot(f.module.id(), f.now.plusSeconds(3600), f.now.plusSeconds(5400));

        assertThrows(IllegalArgumentException.class, () -> f.service.createSlot(f.module.id(),
                f.now.plusSeconds(4500), f.now.plusSeconds(6300)));
    }

    @Test
    void createSlot_overlappingBookedSlot_rejectsRequest() {
        var f = new TutorFixture();
        ConsultationSlot existing = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(existing.withStatus(SlotStatus.BOOKED));

        assertThrows(IllegalArgumentException.class, () -> f.service.createSlot(f.module.id(),
                f.now.plusSeconds(4500), f.now.plusSeconds(6300)));
    }

    @Test
    void createSlot_adjacentSlot_createsAvailableSlot() {
        var f = new TutorFixture();
        f.service.createSlot(f.module.id(), f.now.plusSeconds(3600), f.now.plusSeconds(5400));

        ConsultationSlot adjacent = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(5400), f.now.plusSeconds(7200));

        assertEquals(SlotStatus.AVAILABLE, adjacent.status());
    }

    @Test
    void createSlot_overlappingCancelledSlot_createsAvailableSlot() {
        var f = new TutorFixture();
        ConsultationSlot existing = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(existing.withStatus(SlotStatus.CANCELLED));

        ConsultationSlot replacement = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(4500), f.now.plusSeconds(6300));

        assertEquals(SlotStatus.AVAILABLE, replacement.status());
    }

    @Test
    void createSlot_overlappingCompletedSlot_createsAvailableSlot() {
        var f = new TutorFixture();
        ConsultationSlot existing = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(existing.withStatus(SlotStatus.COMPLETED));

        ConsultationSlot replacement = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(4500), f.now.plusSeconds(6300));

        assertEquals(SlotStatus.AVAILABLE, replacement.status());
    }

    @Test
    void createSlot_concurrentOverlap_acceptsOnlyOneRequest() throws Exception {
        var f = new TutorFixture();
        Callable<Boolean> attempt = () -> {
            try {
                f.service.createSlot(f.module.id(), f.now.plusSeconds(3600), f.now.plusSeconds(5400));
                return true;
            } catch (IllegalArgumentException conflict) {
                return false;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(java.util.List.of(attempt, attempt));
            long accepted = results.stream().filter(result -> {
                try { return result.get(); } catch (Exception failure) { throw new AssertionError(failure); }
            }).count();
            assertEquals(1, accepted);
        }
        assertEquals(1, f.data.slots().findByTutorId(f.tutor.id()).size());
    }

    @Test
    void cancelSlot_ownedAvailableSlot_marksSlotCancelled() {
        var f = new TutorFixture();
        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));

        ConsultationSlot cancelled = f.service.cancelSlot(slot.id());

        assertEquals(SlotStatus.CANCELLED, cancelled.status());
        assertEquals(SlotStatus.CANCELLED, f.data.slots().findById(slot.id()).orElseThrow().status());
    }

    @Test
    void cancelSlot_otherTutorsSlot_rejectsRequest() {
        var f = new TutorFixture();
        Tutor otherTutor = new Tutor(UUID.randomUUID(), "Grace", "grace@example.edu", true);
        f.data.users().save(otherTutor);
        f.data.modules().assign(new TutorModule(otherTutor.id(), f.module.id()));
        ConsultationSlot slot = f.service(otherTutor.id()).createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));

        assertThrows(IllegalArgumentException.class, () -> f.service.cancelSlot(slot.id()));
    }

    @Test
    void cancelSlot_bookedSlot_rejectsRequest() {
        var f = new TutorFixture();
        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(slot.withStatus(SlotStatus.BOOKED));

        assertThrows(IllegalArgumentException.class, () -> f.service.cancelSlot(slot.id()));
    }

    @Test
    void cancelSlot_completedSlot_rejectsRequest() {
        var f = new TutorFixture();
        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(slot.withStatus(SlotStatus.COMPLETED));

        assertThrows(IllegalArgumentException.class, () -> f.service.cancelSlot(slot.id()));
    }

    @Test
    void cancelSlot_missingSlot_rejectsRequest() {
        var f = new TutorFixture();

        assertThrows(IllegalArgumentException.class, () -> f.service.cancelSlot(UUID.randomUUID()));
    }

    @Test
    void cancelSlot_cancelledSlot_rejectsRequest() {
        var f = new TutorFixture();
        ConsultationSlot slot = f.service.createSlot(f.module.id(),
                f.now.plusSeconds(3600), f.now.plusSeconds(5400));
        f.data.slots().save(slot.withStatus(SlotStatus.CANCELLED));

        assertThrows(IllegalArgumentException.class, () -> f.service.cancelSlot(slot.id()));
    }
}
