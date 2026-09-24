package data.memory;

import data.repository.ModuleRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import model.module.Module;
import model.module.TutorModule;

public final class InMemoryModuleRepository extends InMemoryRepository<Module> implements ModuleRepository {
    private final Set<TutorModule> assignments = new LinkedHashSet<>();

    public InMemoryModuleRepository() { this(new Object()); }

    InMemoryModuleRepository(Object lock) { super(Module::id, lock); }

    @Override
    public Module save(Module module) {
        synchronized (lock) {
            boolean duplicate = findAll().stream().anyMatch(existing ->
                    existing.code().equalsIgnoreCase(module.code()) && !existing.id().equals(module.id()));
            if (duplicate) { throw new IllegalArgumentException("Module code already exists"); }
            return super.save(module);
        }
    }

    @Override
    public void assign(TutorModule assignment) {
        synchronized (lock) {
            Objects.requireNonNull(assignment, "assignment");
            if (findById(assignment.moduleId()).isEmpty()) {
                throw new IllegalArgumentException("Module does not exist");
            }
            assignments.add(assignment);
        }
    }

    @Override
    public boolean unassign(TutorModule assignment) {
        synchronized (lock) {
            return assignments.remove(Objects.requireNonNull(assignment, "assignment"));
        }
    }

    @Override
    public List<TutorModule> findAssignmentsByTutor(UUID tutorId) {
        synchronized (lock) {
            Objects.requireNonNull(tutorId, "tutorId");
            return assignments.stream().filter(a -> a.tutorId().equals(tutorId)).toList();
        }
    }

    @Override
    public List<TutorModule> findAssignments() {
        synchronized (lock) {
            return List.copyOf(assignments);
        }
    }
}
