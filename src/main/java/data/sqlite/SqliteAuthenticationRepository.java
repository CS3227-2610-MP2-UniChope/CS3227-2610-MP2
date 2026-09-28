package data.sqlite;

import data.repository.AuthenticationRepository;
import data.repository.CredentialHash;
import data.repository.StoredCredential;
import data.repository.UserRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import model.Validation;
import model.user.Admin;
import model.user.Role;
import model.user.User;

final class SqliteAuthenticationRepository implements AuthenticationRepository {
    private final SqliteDatabase database;
    private final UserRepository users;

    SqliteAuthenticationRepository(SqliteDatabase database, UserRepository users) {
        this.database = Objects.requireNonNull(database, "database");
        this.users = Objects.requireNonNull(users, "users");
    }

    @Override
    public Optional<StoredCredential> findCredentialByEmail(String email) {
        String normalized = Validation.text(email, "email");
        return database.run(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT id, name, email, role, active, "
                    + "password_hash, must_change_password FROM users "
                    + "WHERE email = ? AND password_hash IS NOT NULL")) {
                statement.setString(1, normalized);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) { return Optional.empty(); }
                    User user = SqliteUserRepository.map(results);
                    return Optional.of(new StoredCredential(user, results.getString("password_hash"),
                            results.getInt("must_change_password") == 1));
                }
            }
        });
    }

    @Override
    public User createAccount(User user, String encodedHash, boolean mustChangePassword) {
        User candidate = Objects.requireNonNull(user, "user");
        String hash = CredentialHash.requireArgon2id(encodedHash);
        return database.transaction(() -> {
            if (candidate.role() == Role.ADMIN && !bootstrapComplete()) {
                throw new IllegalArgumentException("Create the first admin through initial setup");
            }
            return createAccountWithinTransaction(candidate, hash, mustChangePassword);
        });
    }

    @Override
    public void updateCredential(UUID userId, String encodedHash, boolean mustChangePassword) {
        UUID id = Objects.requireNonNull(userId, "userId");
        String hash = CredentialHash.requireArgon2id(encodedHash);
        database.transaction(() -> {
            if (users.findById(id).isEmpty()) {
                throw new IllegalArgumentException("User no longer exists");
            }
            writeCredential(id, hash, mustChangePassword);
            return null;
        });
    }

    @Override
    public boolean bootstrapComplete() {
        return database.run(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT value FROM application_metadata WHERE key = 'auth_bootstrap_complete'");
                 ResultSet result = statement.executeQuery()) {
                if (!result.next()) { throw new IllegalStateException("Authentication bootstrap state is missing"); }
                return switch (result.getString("value")) {
                    case "true" -> true;
                    case "false" -> false;
                    default -> throw new IllegalStateException("Authentication bootstrap state is invalid");
                };
            }
        });
    }

    @Override
    public Admin createInitialAdmin(Admin admin, String encodedHash) {
        Admin candidate = Objects.requireNonNull(admin, "admin");
        if (!candidate.isActive()) { throw new IllegalArgumentException("Initial admin must be active"); }
        String hash = CredentialHash.requireArgon2id(encodedHash);
        return database.transaction(() -> {
            if (bootstrapComplete()) {
                throw new IllegalArgumentException("Initial admin setup is already complete");
            }
            createAccountWithinTransaction(candidate, hash, false);
            database.run(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("UPDATE application_metadata "
                        + "SET value = 'true' WHERE key = 'auth_bootstrap_complete' AND value = 'false'")) {
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException("Initial admin setup is already complete");
                    }
                    return null;
                }
            });
            return candidate;
        });
    }

    private void writeCredential(UUID userId, String encodedHash, boolean mustChangePassword) {
        database.run(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE users SET password_hash = ?, "
                    + "must_change_password = ? WHERE id = ?")) {
                statement.setString(1, encodedHash);
                statement.setInt(2, mustChangePassword ? 1 : 0);
                statement.setString(3, userId.toString());
                if (statement.executeUpdate() != 1) { throw new IllegalArgumentException("User no longer exists"); }
                return null;
            }
        });
    }

    private User createAccountWithinTransaction(User user, String encodedHash, boolean mustChangePassword) {
        if (users.findById(user.id()).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        users.save(user);
        writeCredential(user.id(), encodedHash, mustChangePassword);
        return user;
    }

}
