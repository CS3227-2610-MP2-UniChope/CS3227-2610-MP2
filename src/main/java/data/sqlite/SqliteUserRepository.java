package data.sqlite;

import data.repository.UserRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import model.Validation;
import model.user.Admin;
import model.user.Role;
import model.user.Student;
import model.user.Tutor;
import model.user.User;

final class SqliteUserRepository implements UserRepository {
    private final SqliteDatabase database;

    SqliteUserRepository(SqliteDatabase database) { this.database = Objects.requireNonNull(database, "database"); }

    @Override
    public User save(User user) {
        User candidate = Objects.requireNonNull(user, "user");
        return database.run(connection -> {
            findByEmail(candidate.email()).filter(existing -> !existing.id().equals(candidate.id()))
                    .ifPresent(existing -> { throw new IllegalArgumentException("Email already exists"); });
            findById(candidate.id()).filter(existing -> existing.role() != candidate.role())
                    .ifPresent(existing -> { throw new IllegalArgumentException("Cannot change a user's role"); });
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users "
                    + "(id, name, email, role, active) VALUES (?, ?, ?, ?, ?) "
                    + "ON CONFLICT(id) DO UPDATE SET name = excluded.name, email = excluded.email, "
                    + "active = excluded.active")) {
                statement.setString(1, candidate.id().toString());
                statement.setString(2, candidate.name());
                statement.setString(3, candidate.email());
                statement.setString(4, candidate.role().name());
                statement.setInt(5, candidate.isActive() ? 1 : 0);
                statement.executeUpdate();
                return candidate;
            }
        });
    }

    @Override
    public Optional<User> findById(UUID id) {
        UUID userId = Objects.requireNonNull(id, "id");
        return database.run(connection -> findOne(connection,
                "SELECT id, name, email, role, active FROM users WHERE id = ?", userId.toString()));
    }

    @Override
    public List<User> findAll() {
        return database.run(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name, email, role, active FROM users ORDER BY id");
                 ResultSet results = statement.executeQuery()) {
                java.util.ArrayList<User> users = new java.util.ArrayList<>();
                while (results.next()) { users.add(map(results)); }
                return List.copyOf(users);
            }
        });
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String normalized = Validation.text(email, "email");
        return database.run(connection -> findOne(connection,
                "SELECT id, name, email, role, active FROM users WHERE email = ?", normalized));
    }

    private Optional<User> findOne(java.sql.Connection connection, String query, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, value);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(map(results)) : Optional.empty();
            }
        }
    }

    private static User map(ResultSet result) throws SQLException {
        UUID id = UUID.fromString(result.getString("id"));
        String name = result.getString("name");
        String email = result.getString("email");
        boolean active = result.getInt("active") == 1;
        return switch (Role.valueOf(result.getString("role"))) {
        case STUDENT -> new Student(id, name, email, active);
        case TUTOR -> new Tutor(id, name, email, active);
        case ADMIN -> new Admin(id, name, email, active);
        };
    }
}
