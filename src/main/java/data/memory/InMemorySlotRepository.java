package data.memory;

import data.repository.SlotRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import model.consultation.ConsultationSlot;

public final class InMemorySlotRepository extends InMemoryRepository<ConsultationSlot> implements SlotRepository {
    public InMemorySlotRepository() { super(ConsultationSlot::id); }

    @Override
    public List<ConsultationSlot> findByTutorId(UUID tutorId) {
        Objects.requireNonNull(tutorId, "tutorId");
        return findAll().stream().filter(slot -> slot.tutorId().equals(tutorId)).toList();
    }

    @Override
    public List<ConsultationSlot> findByModuleId(UUID moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");
        return findAll().stream().filter(slot -> slot.moduleId().equals(moduleId)).toList();
    }
}
