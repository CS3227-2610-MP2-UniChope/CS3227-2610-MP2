package data.sqlite;

import data.repository.ConsultationLifecycle;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;

final class SqliteConsultationLifecycle implements ConsultationLifecycle {
    private final SqliteDatabase database;
    private final SqliteSlotRepository slots;
    private final SqliteBookingRepository bookings;
    SqliteConsultationLifecycle(SqliteDatabase database, SqliteSlotRepository slots, SqliteBookingRepository bookings) {
        this.database = Objects.requireNonNull(database, "database"); this.slots = Objects.requireNonNull(slots, "slots"); this.bookings = Objects.requireNonNull(bookings, "bookings");
    }
    @Override public <T> T withExclusiveAccess(Supplier<T> operation) { return database.transaction(operation); }
    @Override public Booking completeActiveBooking(UUID tutorId, UUID bookingId) { return database.transaction(() -> {
        Booking booking = bookings.findById(Objects.requireNonNull(bookingId, "bookingId")).orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        ConsultationSlot slot = slots.findById(booking.slotId()).orElseThrow(() -> new IllegalArgumentException("Slot does not exist"));
        if (!slot.tutorId().equals(Objects.requireNonNull(tutorId, "tutorId"))) { throw new IllegalArgumentException("Booking does not belong to tutor"); }
        if (booking.status() != BookingStatus.ACTIVE || slot.status() != SlotStatus.BOOKED) { throw new IllegalArgumentException("Booking and slot are not ready for completion"); }
        Booking completed = bookings.save(booking.withStatus(BookingStatus.COMPLETED)); slots.save(slot.withStatus(SlotStatus.COMPLETED)); return completed;
    }); }
}
