package data.sqlite;

import data.repository.SlotRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;

final class SqliteSlotRepository implements SlotRepository {
    private final SqliteDatabase database;
    SqliteSlotRepository(SqliteDatabase database) { this.database = Objects.requireNonNull(database, "database"); }
    @Override public ConsultationSlot save(ConsultationSlot slot) { ConsultationSlot value = Objects.requireNonNull(slot, "slot"); return database.run(connection -> {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO consultation_slots (id, tutor_id, module_id, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET tutor_id = excluded.tutor_id, module_id = excluded.module_id, start_time = excluded.start_time, end_time = excluded.end_time, status = excluded.status")) {
            statement.setString(1, value.id().toString()); statement.setString(2, value.tutorId().toString()); statement.setString(3, value.moduleId().toString()); statement.setString(4, value.startTime().toString()); statement.setString(5, value.endTime().toString()); statement.setString(6, value.status().name()); statement.executeUpdate(); return value;
        }
    }); }
    @Override public Optional<ConsultationSlot> findById(UUID id) { return database.run(connection -> one(connection, "WHERE id = ?", Objects.requireNonNull(id, "id").toString())); }
    @Override public List<ConsultationSlot> findAll() { return database.run(connection -> many(connection, "", null)); }
    @Override public List<ConsultationSlot> findByTutorId(UUID tutorId) { return database.run(connection -> many(connection, "WHERE tutor_id = ?", Objects.requireNonNull(tutorId, "tutorId").toString())); }
    @Override public List<ConsultationSlot> findByModuleId(UUID moduleId) { return database.run(connection -> many(connection, "WHERE module_id = ?", Objects.requireNonNull(moduleId, "moduleId").toString())); }
    private Optional<ConsultationSlot> one(java.sql.Connection connection, String where, String value) throws java.sql.SQLException { List<ConsultationSlot> slots = many(connection, where, value); return slots.isEmpty() ? Optional.empty() : Optional.of(slots.getFirst()); }
    private List<ConsultationSlot> many(java.sql.Connection connection, String where, String value) throws java.sql.SQLException { try (PreparedStatement statement = connection.prepareStatement("SELECT id, tutor_id, module_id, start_time, end_time, status FROM consultation_slots " + where + " ORDER BY id")) { if (value != null) { statement.setString(1, value); } try (ResultSet rows = statement.executeQuery()) { List<ConsultationSlot> slots = new ArrayList<>(); while (rows.next()) { slots.add(map(rows)); } return List.copyOf(slots); } } }
    private static ConsultationSlot map(ResultSet row) throws java.sql.SQLException { return new ConsultationSlot(UUID.fromString(row.getString("id")), UUID.fromString(row.getString("tutor_id")), UUID.fromString(row.getString("module_id")), Instant.parse(row.getString("start_time")), Instant.parse(row.getString("end_time")), SlotStatus.valueOf(row.getString("status"))); }
}
