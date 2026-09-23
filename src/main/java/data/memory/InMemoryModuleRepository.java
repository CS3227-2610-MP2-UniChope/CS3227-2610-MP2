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

    public InMemoryModuleRepository() { super(Module::id); }

    @Override
    public synchronized Module save(Module module) {
        boolean duplicate = findAll().stream().anyMatch(existing ->
                existing.code().equalsIgnoreCase(module.code()) && !existing.id().equals(module.id()));
        if (duplicate) { throw new IllegalArgumentException("Module code already exists"); }
        return super.save(module);
    }

    @Override
    public synchronized void assign(TutorModule assignment) {
        Objects.requireNonNull(assignment, "assignment");
        if (findById(assignment.moduleId()).isEmpty()) {
            throw new IllegalArgumentException("Module does not exist");
        }
        assignments.add(assignment);
    }

    @Override
    public synchronized boolean unassign(TutorModule assignment) {
        return assignments.remove(Objects.requireNonNull(assignment, "assignment"));
    }

    @Override
    public synchronized List<TutorModule> findAssignmentsByTutor(UUID tutorId) {
        Objects.requireNonNull(tutorId, "tutorId");
        return assignments.stream().filter(a -> a.tutorId().equals(tutorId)).toList();
    }

    @Override
    public synchronized List<TutorModule> findAssignments() { return List.copyOf(assignments); }
}
