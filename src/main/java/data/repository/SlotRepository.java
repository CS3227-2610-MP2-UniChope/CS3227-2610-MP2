package data.repository;

import java.util.List;
import java.util.UUID;
import model.consultation.ConsultationSlot;

public interface SlotRepository extends Repository<ConsultationSlot> {
    List<ConsultationSlot> findByTutorId(UUID tutorId);
    List<ConsultationSlot> findByModuleId(UUID moduleId);
}
