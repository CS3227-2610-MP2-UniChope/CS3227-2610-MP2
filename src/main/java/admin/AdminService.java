package admin;

import data.repository.Repositories;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.module.Module;
import model.module.TutorModule;
import model.user.Role;
import model.user.Student;
import model.user.Tutor;
import model.user.User;
import util.OperationLog;

/** All admin operations revalidate the acting identity inside the shared guard. */
public final class AdminService {
    private final Repositories data;
    private final UUID actorId;
    private final Clock clock;
    private final OperationLog log;

    public AdminService(Repositories data, UUID actorId, Clock clock, OperationLog log) {
        this.data = Objects.requireNonNull(data, "data");
        this.actorId = Objects.requireNonNull(actorId, "actorId");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.log = Objects.requireNonNull(log, "log");
    }

    public AdminSnapshot load() {
        return execute("admin.read", null, () -> new AdminSnapshot(data.users().findAll(),
                data.modules().findAll(), data.modules().findAssignments(),
                data.slots().findAll(), data.bookings().findAll()));
    }

    public User addUser(Role role, String name, String email) {
        UUID id = UUID.randomUUID();
        return execute("admin.user.add", id, () -> {
            User user = switch (Objects.requireNonNull(role, "role")) {
                case STUDENT -> new Student(id, name, email, true);
                case TUTOR -> new Tutor(id, name, email, true);
                case ADMIN -> throw new IllegalArgumentException("Only students and tutors can be added");
            };
            return data.users().save(user);
        });
    }

    public User deactivateUser(UUID id) {
        return execute("admin.user.deactivate", id, () -> {
            User target = data.users().findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User no longer exists"));
            if (target.role() == Role.ADMIN) {
                throw new IllegalArgumentException("Admin accounts cannot be changed here");
            }
            if (target.role() == Role.STUDENT && data.bookings().findByStudentId(id).stream()
                    .anyMatch(b -> b.status() == BookingStatus.ACTIVE)) {
                throw new IllegalArgumentException("Resolve this student's active bookings first");
            }
            if (target.role() == Role.TUTOR) { requireNoConsultations(s -> s.tutorId().equals(id)); }
            return data.users().save(target.withActive(false));
        });
    }

    public Module createModule(String code, String name) {
        UUID id = UUID.randomUUID();
        return execute("admin.module.add", id, () -> data.modules().save(new Module(id, code, name, true)));
    }

    public Module editModule(UUID id, String code, String name) {
        return execute("admin.module.edit", id, () -> {
            Module current = module(id);
            return data.modules().save(new Module(id, code, name, current.isActive()));
        });
    }

    public Module deactivateModule(UUID id) {
        return execute("admin.module.deactivate", id, () -> {
            Module current = module(id);
            requireNoConsultations(s -> s.moduleId().equals(id));
            return data.modules().save(current.withActive(false));
        });
    }

    public TutorModule assign(UUID tutorId, UUID moduleId) {
        return execute("admin.assignment.add", moduleId, () -> {
            User tutor = data.users().findById(tutorId)
                    .orElseThrow(() -> new IllegalArgumentException("Tutor no longer exists"));
            if (tutor.role() != Role.TUTOR || !tutor.isActive()) {
                throw new IllegalArgumentException("Select an active tutor");
            }
            if (!module(moduleId).isActive()) { throw new IllegalArgumentException("Select an active module"); }
            TutorModule assignment = new TutorModule(tutorId, moduleId);
            data.modules().assign(assignment);
            return assignment;
        });
    }

    public boolean unassign(UUID tutorId, UUID moduleId) {
        return execute("admin.assignment.remove", moduleId, () -> {
            TutorModule assignment = new TutorModule(tutorId, moduleId);
            if (!data.modules().findAssignments().contains(assignment)) { return false; }
            requireNoConsultations(s -> s.tutorId().equals(tutorId) && s.moduleId().equals(moduleId));
            return data.modules().unassign(assignment);
        });
    }

    private Module module(UUID id) {
        return data.modules().findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module no longer exists"));
    }

    private void requireNoConsultations(Predicate<ConsultationSlot> affected) {
        var slots = data.slots().findAll().stream().filter(affected).toList();
        Set<UUID> ids = slots.stream().map(ConsultationSlot::id).collect(Collectors.toSet());
        if (data.bookings().findAll().stream().anyMatch(b ->
                ids.contains(b.slotId()) && b.status() == BookingStatus.ACTIVE)) {
            throw new IllegalArgumentException("Resolve affected active bookings before changing this record");
        }
        if (slots.stream().anyMatch(s -> s.status() == SlotStatus.AVAILABLE
                && s.startTime().isAfter(clock.instant()))) {
            throw new IllegalArgumentException("Remove affected future available slots first");
        }
    }

    private <T> T execute(String operation, UUID entityId, Supplier<T> action) {
        T result;
        try {
            result = data.lifecycle().withExclusiveAccess(() -> {
                data.users().findById(actorId).filter(u -> u.role() == Role.ADMIN && u.isActive())
                        .orElseThrow(() -> new SecurityException("An active admin account is required. Sign out."));
                return action.get();
            });
        } catch (RuntimeException failure) {
            log.record(operation, actorId, entityId, failure);
            throw failure;
        }
        log.record(operation, actorId, entityId, null);
        return result;
    }
}
