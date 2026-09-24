package model.module;

import java.util.Objects;
import java.util.UUID;
import model.Validation;

/** Explicitly import this type to distinguish it from java.lang.Module. */
public record Module(UUID id, String code, String name, boolean isActive) {
    public Module {
        Objects.requireNonNull(id, "id");
        code = Validation.text(code, "code");
        name = Validation.text(name, "name");
    }

    public Module withActive(boolean active) {
        return new Module(id, code, name, active);
    }
}
