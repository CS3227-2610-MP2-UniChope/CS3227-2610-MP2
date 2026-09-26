package data.sqlite;

import data.repository.BookingRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationNote;

final class SqliteBookingRepository implements BookingRepository {
    private final SqliteDatabase database;
    SqliteBookingRepository(SqliteDatabase database) { this.database = Objects.requireNonNull(database, "database"); }
    @Override public Booking save(Booking booking) { Booking value = Objects.requireNonNull(booking, "booking"); return database.run(connection -> {
        if (value.status() == BookingStatus.ACTIVE && findBySlotId(value.slotId()).stream().anyMatch(existing -> existing.status() == BookingStatus.ACTIVE && !existing.id().equals(value.id()))) { throw new IllegalArgumentException("Slot already has an active booking"); }
        if (findNoteByBookingId(value.id()).isPresent() && value.status() != BookingStatus.COMPLETED) { throw new IllegalArgumentException("A booking with a note must remain completed"); }
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO bookings (id, student_id, slot_id, created_at, status) VALUES (?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET student_id = excluded.student_id, slot_id = excluded.slot_id, created_at = excluded.created_at, status = excluded.status")) {
            statement.setString(1, value.id().toString()); statement.setString(2, value.studentId().toString()); statement.setString(3, value.slotId().toString()); statement.setString(4, value.createdAt().toString()); statement.setString(5, value.status().name()); statement.executeUpdate(); return value;
        }
    }); }
    @Override public Optional<Booking> findById(UUID id) { return database.run(connection -> one(connection, "WHERE id = ?", Objects.requireNonNull(id, "id").toString())); }
    @Override public List<Booking> findAll() { return database.run(connection -> many(connection, "", null)); }
    @Override public List<Booking> findByStudentId(UUID studentId) { return database.run(connection -> many(connection, "WHERE student_id = ?", Objects.requireNonNull(studentId, "studentId").toString())); }
    @Override public List<Booking> findBySlotId(UUID slotId) { return database.run(connection -> many(connection, "WHERE slot_id = ?", Objects.requireNonNull(slotId, "slotId").toString())); }
    @Override public ConsultationNote saveNote(ConsultationNote note) { ConsultationNote value = Objects.requireNonNull(note, "note"); return database.run(connection -> {
        Booking booking = findById(value.bookingId()).orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        if (booking.status() != BookingStatus.COMPLETED) { throw new IllegalArgumentException("Notes require a completed booking"); }
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO consultation_notes (booking_id, content, updated_at) VALUES (?, ?, ?) ON CONFLICT(booking_id) DO UPDATE SET content = excluded.content, updated_at = excluded.updated_at")) {
            statement.setString(1, value.bookingId().toString()); statement.setString(2, value.content()); statement.setString(3, value.updatedAt().toString()); statement.executeUpdate(); return value;
        }
    }); }
    @Override public Optional<ConsultationNote> findNoteByBookingId(UUID bookingId) { return database.run(connection -> {
        try (PreparedStatement statement = connection.prepareStatement("SELECT booking_id, content, updated_at FROM consultation_notes WHERE booking_id = ?")) { statement.setString(1, Objects.requireNonNull(bookingId, "bookingId").toString()); try (ResultSet rows = statement.executeQuery()) { return rows.next() ? Optional.of(new ConsultationNote(UUID.fromString(rows.getString(1)), rows.getString(2), Instant.parse(rows.getString(3)))) : Optional.empty(); } }
    }); }
    private Optional<Booking> one(java.sql.Connection connection, String where, String value) throws java.sql.SQLException { List<Booking> bookings = many(connection, where, value); return bookings.isEmpty() ? Optional.empty() : Optional.of(bookings.getFirst()); }
    private List<Booking> many(java.sql.Connection connection, String where, String value) throws java.sql.SQLException { try (PreparedStatement statement = connection.prepareStatement("SELECT id, student_id, slot_id, created_at, status FROM bookings " + where + " ORDER BY id")) { if (value != null) { statement.setString(1, value); } try (ResultSet rows = statement.executeQuery()) { List<Booking> bookings = new ArrayList<>(); while (rows.next()) { bookings.add(new Booking(UUID.fromString(rows.getString("id")), UUID.fromString(rows.getString("student_id")), UUID.fromString(rows.getString("slot_id")), Instant.parse(rows.getString("created_at")), BookingStatus.valueOf(rows.getString("status")))); } return List.copyOf(bookings); } } }
}
