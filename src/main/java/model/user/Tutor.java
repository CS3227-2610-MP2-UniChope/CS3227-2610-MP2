package model.user;

import java.util.Objects;
import java.util.UUID;
import model.Validation;

public record Tutor(UUID id, String name, String email, boolean isActive) implements User {
    public Tutor {
        Objects.requireNonNull(id, "id");
        name = Validation.text(name, "name");
        email = Validation.email(email);
    }

    @Override
    public Role role() { return Role.TUTOR; }

    @Override
    public Tutor withActive(boolean active) {
        return new Tutor(id, name, email, active);
    }
}
