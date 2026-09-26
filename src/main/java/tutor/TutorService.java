package tutor;

import data.repository.Repositories;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.module.Module;
import model.module.TutorModule;
import model.user.Tutor;
import util.OperationLog;

/** Tutor-role business rules independent of the JavaFX user interface. */
public final class TutorService {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private final Repositories data;
    private final UUID actorId;
    private final Clock clock;
    private final OperationLog log;

    public TutorService(Repositories data, UUID actorId, Clock clock, OperationLog log) {
        this.data = Objects.requireNonNull(data, "data");
        this.actorId = Objects.requireNonNull(actorId, "actorId");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.log = Objects.requireNonNull(log, "log");
    }

    public ConsultationSlot createSlot(UUID moduleId, Instant startTime, Instant endTime) {
        UUID slotId = UUID.randomUUID();
        return execute("tutor.slot.create", slotId, () -> createSlot(slotId, moduleId, startTime, endTime));
    }

    public ConsultationSlot cancelSlot(UUID slotId) {
        UUID validatedSlotId = Objects.requireNonNull(slotId, "slotId");
        return execute("tutor.slot.cancel", validatedSlotId, () -> {
            Tutor tutor = requireActiveTutor();
            ConsultationSlot slot = data.slots().findById(validatedSlotId)
                    .orElseThrow(() -> new IllegalArgumentException("Slot does not exist"));
            if (!slot.tutorId().equals(tutor.id()) || slot.status() != SlotStatus.AVAILABLE) {
                throw new IllegalArgumentException("Only an owned available slot can be cancelled");
            }
            return data.slots().save(slot.withStatus(SlotStatus.CANCELLED));
        });
    }

    public List<ConsultationSlot> findUpcomingSlots(LocalDate date) {
        LocalDate selectedDate = Objects.requireNonNull(date, "date");
        return execute("tutor.slot.upcoming", null, () -> {
            Tutor tutor = requireActiveTutor();
            return data.slots().findByTutorId(tutor.id()).stream()
                    .filter(slot -> !slot.startTime().isBefore(clock.instant()))
                    .filter(slot -> slot.status() == SlotStatus.AVAILABLE || slot.status() == SlotStatus.BOOKED)
                    .filter(slot -> slot.startTime().atZone(SINGAPORE).toLocalDate().equals(selectedDate))
                    .sorted(Comparator.comparing(ConsultationSlot::startTime).thenComparing(ConsultationSlot::id))
                    .toList();
        });
    }

    private ConsultationSlot createSlot(UUID slotId, UUID moduleId, Instant startTime, Instant endTime) {
        Tutor tutor = requireActiveTutor();
        Module module = data.modules().findById(Objects.requireNonNull(moduleId, "moduleId"))
                .orElseThrow(() -> new IllegalArgumentException("Module does not exist"));
        if (!module.isActive()) {
            throw new IllegalArgumentException("Module is inactive");
        }
        if (!data.modules().findAssignmentsByTutor(tutor.id())
                .contains(new TutorModule(tutor.id(), module.id()))) {
            throw new IllegalArgumentException("Tutor is not assigned to module");
        }
        if (Objects.requireNonNull(startTime, "startTime").isBefore(clock.instant())) {
            throw new IllegalArgumentException("Slot cannot start in the past");
        }
        Instant validatedEndTime = Objects.requireNonNull(endTime, "endTime");
        boolean overlapsAvailableSlot = data.slots().findByTutorId(tutor.id()).stream()
                .filter(slot -> slot.status() == SlotStatus.AVAILABLE || slot.status() == SlotStatus.BOOKED)
                .anyMatch(slot -> startTime.isBefore(slot.endTime()) && validatedEndTime.isAfter(slot.startTime()));
        if (overlapsAvailableSlot) {
            throw new IllegalArgumentException("Slot overlaps an existing slot");
        }
        ConsultationSlot slot = new ConsultationSlot(slotId, tutor.id(), module.id(),
                startTime, validatedEndTime, SlotStatus.AVAILABLE);
        return data.slots().save(slot);
    }

    private Tutor requireActiveTutor() {
        return data.users().findById(actorId)
                .filter(Tutor.class::isInstance)
                .map(Tutor.class::cast)
                .filter(Tutor::isActive)
                .orElseThrow(() -> new SecurityException("An active tutor account is required. Sign out."));
    }

    private <T> T execute(String operation, UUID entityId, Supplier<T> action) {
        T result;
        try {
            result = data.lifecycle().withExclusiveAccess(action);
        } catch (RuntimeException failure) {
            log.record(operation, actorId, entityId, failure);
            throw failure;
        }
        log.record(operation, actorId, entityId, null);
        return result;
    }
}
