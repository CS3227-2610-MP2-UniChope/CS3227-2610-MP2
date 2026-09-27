package data.sqlite;

import data.repository.ConsultationLifecycle;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.module.TutorModule;
import model.user.Role;
import model.user.User;

final class SqliteConsultationLifecycle implements ConsultationLifecycle {
    private final SqliteDatabase database;
    private final SqliteUserRepository users;
    private final SqliteModuleRepository modules;
    private final SqliteSlotRepository slots;
    private final SqliteBookingRepository bookings;

    SqliteConsultationLifecycle(SqliteDatabase database, SqliteUserRepository users,
                                SqliteModuleRepository modules, SqliteSlotRepository slots,
                                SqliteBookingRepository bookings) {
        this.database = Objects.requireNonNull(database, "database");
        this.users = Objects.requireNonNull(users, "users");
        this.modules = Objects.requireNonNull(modules, "modules");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.bookings = Objects.requireNonNull(bookings, "bookings");
    }

    @Override
    public <T> T withExclusiveAccess(Supplier<T> operation) {
        return database.transaction(operation);
    }

    @Override
    public Booking bookAvailableSlot(UUID studentId, UUID slotId, Instant now) {
        return database.transaction(() -> {
            requireActiveStudent(studentId);
            Objects.requireNonNull(now, "now");
            ConsultationSlot slot = slots.findById(Objects.requireNonNull(slotId, "slotId"))
                    .orElseThrow(() -> new IllegalArgumentException("Slot no longer exists"));
            if (slot.status() != SlotStatus.AVAILABLE || !slot.startTime().isAfter(now)) {
                throw new IllegalArgumentException("Select a future available slot");
            }
            User tutor = users.findById(slot.tutorId())
                    .orElseThrow(() -> new IllegalArgumentException("Tutor no longer exists"));
            if (tutor.role() != Role.TUTOR || !tutor.isActive()) {
                throw new IllegalArgumentException("Tutor is no longer active");
            }
            if (modules.findById(slot.moduleId()).filter(m -> m.isActive()).isEmpty()
                    || !modules.findAssignments().contains(new TutorModule(slot.tutorId(), slot.moduleId()))) {
                throw new IllegalArgumentException("Module or tutor assignment is no longer active");
            }
            if (bookings.findBySlotId(slot.id()).stream().anyMatch(b -> b.status() == BookingStatus.ACTIVE)) {
                throw new IllegalArgumentException("Slot already has an active booking");
            }
            boolean overlaps = bookings.findByStudentId(studentId).stream()
                    .filter(b -> b.status() == BookingStatus.ACTIVE)
                    .map(b -> slots.findById(b.slotId()).orElse(null))
                    .filter(Objects::nonNull)
                    .anyMatch(other -> slot.startTime().isBefore(other.endTime())
                            && other.startTime().isBefore(slot.endTime()));
            if (overlaps) { throw new IllegalArgumentException("You already have a booking at this time"); }
            Booking booking = bookings.save(new Booking(UUID.randomUUID(), studentId, slot.id(), now,
                    BookingStatus.ACTIVE));
            slots.save(slot.withStatus(SlotStatus.BOOKED));
            return booking;
        });
    }

    @Override
    public Booking cancelActiveBooking(UUID studentId, UUID bookingId, Instant now) {
        return database.transaction(() -> {
            requireActiveStudent(studentId);
            Objects.requireNonNull(now, "now");
            Booking booking = bookings.findById(Objects.requireNonNull(bookingId, "bookingId"))
                    .orElseThrow(() -> new IllegalArgumentException("Booking no longer exists"));
            if (!booking.studentId().equals(studentId)) {
                throw new IllegalArgumentException("Booking does not belong to student");
            }
            ConsultationSlot slot = slots.findById(booking.slotId())
                    .orElseThrow(() -> new IllegalArgumentException("Slot no longer exists"));
            if (booking.status() != BookingStatus.ACTIVE || slot.status() != SlotStatus.BOOKED
                    || !slot.startTime().isAfter(now)) {
                throw new IllegalArgumentException("Only future active bookings can be cancelled");
            }
            Booking cancelled = bookings.save(booking.withStatus(BookingStatus.CANCELLED));
            slots.save(slot.withStatus(SlotStatus.AVAILABLE));
            return cancelled;
        });
    }

    @Override
    public Booking completeActiveBooking(UUID tutorId, UUID bookingId) {
        return database.transaction(() -> {
            Booking booking = bookings.findById(Objects.requireNonNull(bookingId, "bookingId"))
                    .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
            ConsultationSlot slot = slots.findById(booking.slotId())
                    .orElseThrow(() -> new IllegalArgumentException("Slot does not exist"));
            if (!slot.tutorId().equals(Objects.requireNonNull(tutorId, "tutorId"))) {
                throw new IllegalArgumentException("Booking does not belong to tutor");
            }
            if (booking.status() != BookingStatus.ACTIVE || slot.status() != SlotStatus.BOOKED) {
                throw new IllegalArgumentException("Booking and slot are not ready for completion");
            }
            Booking completed = bookings.save(booking.withStatus(BookingStatus.COMPLETED));
            slots.save(slot.withStatus(SlotStatus.COMPLETED));
            return completed;
        });
    }

    private void requireActiveStudent(UUID studentId) {
        users.findById(Objects.requireNonNull(studentId, "studentId"))
                .filter(u -> u.role() == Role.STUDENT && u.isActive())
                .orElseThrow(() -> new SecurityException("An active student account is required. Sign out."));
    }
}
