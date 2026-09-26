package tutor;

import java.time.Instant;
import java.util.UUID;
import model.consultation.SlotStatus;

/** A tutor-owned slot formatted with the fields required by the Slots table. */
public record TutorSlotView(UUID slotId, String moduleCode, Instant startTime, Instant endTime,
                            SlotStatus status) { }
