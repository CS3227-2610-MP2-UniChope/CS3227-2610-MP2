package model.user;

import java.util.UUID;

/** Immutable common user contract; each subtype fixes its role. */
public sealed interface User permits Student, Tutor, Admin {
    UUID id();
    String name();
    String email();
    Role role();
    boolean isActive();
    User withActive(boolean active);
}
