package data.sqlite;

import data.repository.AuthenticationRepository;
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
        SqliteAuthenticationRepository authentication = new SqliteAuthenticationRepository(coordinator, users);
        return new Repositories(users, modules, slots, bookings,
                new SqliteConsultationLifecycle(coordinator, users, modules, slots, bookings), authentication);
    }

    /** Opens the credential persistence boundary against the same local database file. */
    public static AuthenticationRepository openAuthentication(Path database) {
        SqliteDatabase coordinator = new SqliteDatabase(Objects.requireNonNull(database, "database"));
        return new SqliteAuthenticationRepository(coordinator, new SqliteUserRepository(coordinator));
    }
}
