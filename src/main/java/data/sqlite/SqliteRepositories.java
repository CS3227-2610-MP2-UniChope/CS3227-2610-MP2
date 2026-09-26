package data.sqlite;

import data.repository.Repositories;
import java.nio.file.Path;
import java.util.Objects;

/** Opens one shared SQLite-backed repository bundle. */
public final class SqliteRepositories {
    private SqliteRepositories() { }

    public static Repositories open(Path database) {
        SqliteDatabase coordinator = new SqliteDatabase(Objects.requireNonNull(database, "database"));
        SqliteUserRepository users = new SqliteUserRepository(coordinator);
        SqliteModuleRepository modules = new SqliteModuleRepository(coordinator);
        SqliteSlotRepository slots = new SqliteSlotRepository(coordinator);
        SqliteBookingRepository bookings = new SqliteBookingRepository(coordinator);
        return new Repositories(users, modules, slots, bookings,
                new SqliteConsultationLifecycle(coordinator, slots, bookings));
    }
}
