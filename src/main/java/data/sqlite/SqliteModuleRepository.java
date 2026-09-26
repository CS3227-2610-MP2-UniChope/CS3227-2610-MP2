package data.sqlite;

import data.repository.ModuleRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import model.module.Module;
import model.module.TutorModule;

final class SqliteModuleRepository implements ModuleRepository {
    private final SqliteDatabase database;
    SqliteModuleRepository(SqliteDatabase database) { this.database = Objects.requireNonNull(database, "database"); }

    @Override public Module save(Module module) {
        Module candidate = Objects.requireNonNull(module, "module");
        return database.run(connection -> {
            boolean duplicate = findAll().stream().anyMatch(existing -> existing.code().equalsIgnoreCase(candidate.code())
                    && !existing.id().equals(candidate.id()));
            if (duplicate) { throw new IllegalArgumentException("Module code already exists"); }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO modules (id, code, name, active) "
                    + "VALUES (?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET code = excluded.code, name = excluded.name, active = excluded.active")) {
                statement.setString(1, candidate.id().toString()); statement.setString(2, candidate.code());
                statement.setString(3, candidate.name()); statement.setInt(4, candidate.isActive() ? 1 : 0);
                statement.executeUpdate(); return candidate;
            }
        });
    }
    @Override public Optional<Module> findById(UUID id) { return database.run(connection -> findOne(connection,
            "SELECT id, code, name, active FROM modules WHERE id = ?", Objects.requireNonNull(id, "id").toString())); }
    @Override public List<Module> findAll() { return database.run(connection -> {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, code, name, active FROM modules ORDER BY id"); ResultSet rows = statement.executeQuery()) {
            List<Module> modules = new ArrayList<>(); while (rows.next()) { modules.add(map(rows)); } return List.copyOf(modules);
        }
    }); }
    @Override public void assign(TutorModule assignment) { TutorModule value = Objects.requireNonNull(assignment, "assignment"); database.run(connection -> {
        if (findById(value.moduleId()).isEmpty()) { throw new IllegalArgumentException("Module does not exist"); }
        try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO tutor_modules (tutor_id, module_id) VALUES (?, ?)")) {
            statement.setString(1, value.tutorId().toString()); statement.setString(2, value.moduleId().toString()); statement.executeUpdate(); return null;
        }
    }); }
    @Override public boolean unassign(TutorModule assignment) { TutorModule value = Objects.requireNonNull(assignment, "assignment"); return database.run(connection -> {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM tutor_modules WHERE tutor_id = ? AND module_id = ?")) {
            statement.setString(1, value.tutorId().toString()); statement.setString(2, value.moduleId().toString()); return statement.executeUpdate() == 1;
        }
    }); }
    @Override public List<TutorModule> findAssignmentsByTutor(UUID tutorId) { String tutor = Objects.requireNonNull(tutorId, "tutorId").toString(); return database.run(connection -> assignments(connection, "WHERE tutor_id = ?", tutor)); }
    @Override public List<TutorModule> findAssignments() { return database.run(connection -> assignments(connection, "", null)); }
    private Optional<Module> findOne(java.sql.Connection connection, String sql, String value) throws java.sql.SQLException { try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.setString(1, value); try (ResultSet rows = statement.executeQuery()) { return rows.next() ? Optional.of(map(rows)) : Optional.empty(); } } }
    private List<TutorModule> assignments(java.sql.Connection connection, String where, String tutor) throws java.sql.SQLException { try (PreparedStatement statement = connection.prepareStatement("SELECT tutor_id, module_id FROM tutor_modules " + where + " ORDER BY tutor_id, module_id")) { if (tutor != null) { statement.setString(1, tutor); } try (ResultSet rows = statement.executeQuery()) { List<TutorModule> values = new ArrayList<>(); while (rows.next()) { values.add(new TutorModule(UUID.fromString(rows.getString(1)), UUID.fromString(rows.getString(2)))); } return List.copyOf(values); } } }
    private static Module map(ResultSet row) throws java.sql.SQLException { return new Module(UUID.fromString(row.getString("id")), row.getString("code"), row.getString("name"), row.getInt("active") == 1); }
}
