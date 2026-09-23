package model.module;

import java.util.Objects;
import java.util.UUID;

/** Composite identity: one assignment per tutor/module pair. */
public record TutorModule(UUID tutorId, UUID moduleId) {
    public TutorModule {
        Objects.requireNonNull(tutorId, "tutorId");
        Objects.requireNonNull(moduleId, "moduleId");
    }
}
