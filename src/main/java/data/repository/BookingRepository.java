package data.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import model.consultation.Booking;
import model.consultation.ConsultationNote;

public interface BookingRepository extends Repository<Booking> {
    /**
     * Rejects a second ACTIVE booking for a slot and a non-COMPLETED booking with
     * an existing note. This alone is not an atomic booking/slot transaction.
     */
    @Override
    Booking save(Booking booking);

    List<Booking> findByStudentId(UUID studentId);
    List<Booking> findBySlotId(UUID slotId);
    /** Inserts or replaces the single note for an existing completed booking. */
    ConsultationNote saveNote(ConsultationNote note);
    Optional<ConsultationNote> findNoteByBookingId(UUID bookingId);
}
