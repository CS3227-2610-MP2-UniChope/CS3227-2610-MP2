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
import org.sqlite.SQLiteConfig;

/** Shared connection and transaction coordinator for one SQLite repository bundle. */
final class SqliteDatabase {
    private static final int SCHEMA_VERSION = 2;
    private static final int BUSY_TIMEOUT_MILLIS = 5000;
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
        try (Connection opened = openConnection(false)) {
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
        try (Connection connection = openConnection(true)) {
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
        transaction(() -> {
            run(connection -> {
                int version;
                try (Statement statement = connection.createStatement();
                     var result = statement.executeQuery("PRAGMA user_version")) {
                    version = result.getInt(1);
                }
                try (Statement statement = connection.createStatement()) {
                    if (version == 0) {
                        createSchema(statement);
                    } else if (version != SCHEMA_VERSION) {
                        throw new IllegalStateException("Unsupported SQLite schema version: " + version);
                    }
                    statement.executeUpdate("PRAGMA user_version = " + SCHEMA_VERSION);
                }
                return null;
            });
            return null;
        });
    }

    private static void createSchema(Statement statement) throws SQLException {
        statement.executeUpdate("CREATE TABLE users (id TEXT PRIMARY KEY, name TEXT NOT NULL, "
                + "email TEXT NOT NULL COLLATE NOCASE UNIQUE, role TEXT NOT NULL, "
                + "active INTEGER NOT NULL CHECK (active IN (0, 1)), password_hash TEXT, "
                + "must_change_password INTEGER NOT NULL DEFAULT 1 CHECK (must_change_password IN (0, 1)))");
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
        createAuthenticationMetadata(statement);
    }

    private static void createAuthenticationMetadata(Statement statement) throws SQLException {
        statement.executeUpdate("CREATE TABLE application_metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
        statement.executeUpdate("INSERT INTO application_metadata (key, value) "
                + "VALUES ('auth_bootstrap_complete', 'false')");
    }

    private Connection openConnection(boolean transaction) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setBusyTimeout(BUSY_TIMEOUT_MILLIS);
        if (transaction) { config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE); }
        Connection connection = DriverManager.getConnection(url, config.toProperties());
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
