package data.repository;

import java.util.Objects;

/** Inject one shared bundle into the shell and role features. */
public record Repositories(UserRepository users, ModuleRepository modules,
                           SlotRepository slots, BookingRepository bookings) {
    public Repositories {
        Objects.requireNonNull(users, "users");
        Objects.requireNonNull(modules, "modules");
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(bookings, "bookings");
    }
}
