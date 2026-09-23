package data.repository;

import java.util.List;
import java.util.UUID;
import model.module.Module;
import model.module.TutorModule;

public interface ModuleRepository extends Repository<Module> {
    /** Rejects duplicate module codes ignoring case. */
    @Override
    Module save(Module module);

    /** Idempotent assignment; caller must validate that tutor and module are active. */
    void assign(TutorModule assignment);
    boolean unassign(TutorModule assignment);
    List<TutorModule> findAssignmentsByTutor(UUID tutorId);
    List<TutorModule> findAssignments();
}
