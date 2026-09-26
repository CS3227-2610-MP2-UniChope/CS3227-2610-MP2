package data.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Shared connection and transaction coordinator for one SQLite repository bundle. */
final class SqliteDatabase {
    private static final int SCHEMA_VERSION = 1;
    private final String url;
    private final ReentrantLock lock = new ReentrantLock();
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    SqliteDatabase(Path database) {
        Path absolute = Objects.requireNonNull(database, "database").toAbsolutePath();
        try {
            Path parent = absolute.getParent();
            if (parent != null) { Files.createDirectories(parent); }
        } catch (IOException failure) {
            throw new IllegalStateException("Could not create database directory", failure);
        }
        url = "jdbc:sqlite:" + absolute;
        initialize();
    }

    <T> T run(SqlFunction<T> operation) {
        Objects.requireNonNull(operation, "operation");
        Connection connection = transactionConnection.get();
        if (connection != null) { return apply(operation, connection); }
        lock.lock();
        try (Connection opened = openConnection()) {
            return apply(operation, opened);
        } catch (SQLException failure) {
            throw new IllegalStateException("SQLite operation failed", failure);
        } finally {
            lock.unlock();
        }
    }

    <T> T transaction(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation");
        if (transactionConnection.get() != null) { return operation.get(); }
        lock.lock();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
            try {
                T result = operation.get();
                connection.commit();
                return result;
            } catch (RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            } finally {
                transactionConnection.remove();
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("SQLite transaction failed", failure);
        } finally {
            lock.unlock();
        }
    }

    private void initialize() {
        lock.lock();
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            int version = statement.executeQuery("PRAGMA user_version").getInt(1);
            if (version == 0) {
                statement.executeUpdate("CREATE TABLE users (id TEXT PRIMARY KEY, name TEXT NOT NULL, "
                        + "email TEXT NOT NULL COLLATE NOCASE UNIQUE, role TEXT NOT NULL, "
                        + "active INTEGER NOT NULL CHECK (active IN (0, 1)))");
                statement.executeUpdate("CREATE TABLE modules (id TEXT PRIMARY KEY, code TEXT NOT NULL "
                        + "COLLATE NOCASE UNIQUE, name TEXT NOT NULL, active INTEGER NOT NULL "
                        + "CHECK (active IN (0, 1)))");
                statement.executeUpdate("CREATE TABLE tutor_modules (tutor_id TEXT NOT NULL, module_id TEXT NOT NULL, "
                        + "PRIMARY KEY (tutor_id, module_id))");
                statement.executeUpdate("CREATE TABLE consultation_slots (id TEXT PRIMARY KEY, tutor_id TEXT NOT NULL, "
                        + "module_id TEXT NOT NULL, start_time TEXT NOT NULL, end_time TEXT NOT NULL, "
                        + "status TEXT NOT NULL)");
                statement.executeUpdate("CREATE TABLE bookings (id TEXT PRIMARY KEY, student_id TEXT NOT NULL, "
                        + "slot_id TEXT NOT NULL, created_at TEXT NOT NULL, status TEXT NOT NULL, "
                        + "CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED')))");
                statement.executeUpdate("CREATE UNIQUE INDEX active_booking_per_slot ON bookings(slot_id) "
                        + "WHERE status = 'ACTIVE'");
                statement.executeUpdate("CREATE TABLE consultation_notes (booking_id TEXT PRIMARY KEY, content TEXT NOT NULL, "
                        + "updated_at TEXT NOT NULL)");
                statement.executeUpdate("PRAGMA user_version = " + SCHEMA_VERSION);
            } else if (version != SCHEMA_VERSION) {
                throw new IllegalStateException("Unsupported SQLite schema version: " + version);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("Could not initialize SQLite database", failure);
        } finally {
            lock.unlock();
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private static <T> T apply(SqlFunction<T> operation, Connection connection) {
        try {
            return operation.apply(connection);
        } catch (SQLException failure) {
            throw new IllegalStateException("SQLite operation failed", failure);
        }
    }

    private static void rollback(Connection connection, RuntimeException failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    @FunctionalInterface
    interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }
}
