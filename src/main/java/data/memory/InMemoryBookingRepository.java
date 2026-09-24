package data.memory;

import data.repository.BookingRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationNote;

public final class InMemoryBookingRepository extends InMemoryRepository<Booking> implements BookingRepository {
    private final Map<UUID, ConsultationNote> notes = new HashMap<>();

    public InMemoryBookingRepository() { this(new Object()); }

    InMemoryBookingRepository(Object lock) { super(Booking::id, lock); }

    @Override
    public Booking save(Booking booking) {
        synchronized (lock) {
            if (booking.status() == BookingStatus.ACTIVE && findBySlotId(booking.slotId()).stream()
                    .anyMatch(existing -> existing.status() == BookingStatus.ACTIVE
                            && !existing.id().equals(booking.id()))) {
                throw new IllegalArgumentException("Slot already has an active booking");
            }
            if (notes.containsKey(booking.id()) && booking.status() != BookingStatus.COMPLETED) {
                throw new IllegalArgumentException("A booking with a note must remain completed");
            }
            return super.save(booking);
        }
    }

    @Override
    public List<Booking> findByStudentId(UUID studentId) {
        Objects.requireNonNull(studentId, "studentId");
        return findAll().stream().filter(b -> b.studentId().equals(studentId)).toList();
    }

    @Override
    public List<Booking> findBySlotId(UUID slotId) {
        Objects.requireNonNull(slotId, "slotId");
        return findAll().stream().filter(b -> b.slotId().equals(slotId)).toList();
    }

    @Override
    public ConsultationNote saveNote(ConsultationNote note) {
        synchronized (lock) {
            Objects.requireNonNull(note, "note");
            Booking booking = findById(note.bookingId())
                    .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
            if (booking.status() != BookingStatus.COMPLETED) {
                throw new IllegalArgumentException("Notes require a completed booking");
            }
            notes.put(note.bookingId(), note);
            return note;
        }
    }

    @Override
    public Optional<ConsultationNote> findNoteByBookingId(UUID bookingId) {
        synchronized (lock) {
            return Optional.ofNullable(notes.get(Objects.requireNonNull(bookingId, "bookingId")));
        }
    }
}
