package student;

import data.repository.Repositories;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.module.Module;
import model.module.TutorModule;
import model.user.Role;
import model.user.User;
import util.OperationLog;

/** Student-facing queries and actions; storage owns atomic booking transitions. */
public final class StudentService {
    public record SlotRow(UUID id, String module, String tutor, Instant start, Instant end) { }
    public record BookingRow(UUID id, String module, String tutor, Instant start, Instant end,
                             BookingStatus status) { }
    public record Snapshot(List<SlotRow> available, List<BookingRow> bookings) { }

    private final Repositories data;
    private final UUID studentId;
    private final Clock clock;
    private final OperationLog log;

    public StudentService(Repositories data, UUID studentId, Clock clock, OperationLog log) {
        this.data = Objects.requireNonNull(data, "data");
        this.studentId = Objects.requireNonNull(studentId, "studentId");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.log = Objects.requireNonNull(log, "log");
    }

    public Snapshot load() {
        return data.lifecycle().withExclusiveAccess(() -> {
            requireStudent();
            Instant now = clock.instant();
            var users = data.users().findAll().stream().collect(java.util.stream.Collectors.toMap(User::id, u -> u));
            var modules = data.modules().findAll().stream().collect(java.util.stream.Collectors.toMap(Module::id, m -> m));
            var slots = data.slots().findAll();
            var slotById = slots.stream().collect(java.util.stream.Collectors.toMap(ConsultationSlot::id, s -> s));
            var assigned = data.modules().findAssignments();
            var activeSlotIds = data.bookings().findAll().stream().filter(b -> b.status() == BookingStatus.ACTIVE)
                    .map(Booking::slotId).collect(java.util.stream.Collectors.toSet());
            List<SlotRow> available = slots.stream().filter(s -> s.status() == SlotStatus.AVAILABLE
                            && s.startTime().isAfter(now) && !activeSlotIds.contains(s.id()))
                    .filter(s -> activeTutor(users.get(s.tutorId())) && activeModule(modules.get(s.moduleId()))
                            && assigned.contains(new TutorModule(s.tutorId(), s.moduleId())))
                    .map(s -> new SlotRow(s.id(), modules.get(s.moduleId()).code(),
                            users.get(s.tutorId()).name(), s.startTime(), s.endTime()))
                    .sorted(Comparator.comparing(SlotRow::start).thenComparing(SlotRow::id)).toList();
            List<BookingRow> bookings = data.bookings().findByStudentId(studentId).stream().map(b -> {
                ConsultationSlot slot = slotById.get(b.slotId());
                Module module = slot == null ? null : modules.get(slot.moduleId());
                User tutor = slot == null ? null : users.get(slot.tutorId());
                return new BookingRow(b.id(), module == null ? "Unavailable" : module.code(),
                        tutor == null ? "Unavailable" : tutor.name(),
                        slot == null ? null : slot.startTime(), slot == null ? null : slot.endTime(), b.status());
            }).sorted(Comparator.comparing(BookingRow::start, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(BookingRow::id)).toList();
            return new Snapshot(available, bookings);
        });
    }

    public Booking book(UUID slotId) {
        try {
            Booking result = data.lifecycle().bookAvailableSlot(studentId, slotId, clock.instant());
            log.record("student.booking.create", studentId, slotId, null);
            return result;
        } catch (RuntimeException failure) {
            log.record("student.booking.create", studentId, slotId, failure);
            throw failure;
        }
    }

    public Booking cancel(UUID bookingId) {
        try {
            Booking result = data.lifecycle().cancelActiveBooking(studentId, bookingId, clock.instant());
            log.record("student.booking.cancel", studentId, bookingId, null);
            return result;
        } catch (RuntimeException failure) {
            log.record("student.booking.cancel", studentId, bookingId, failure);
            throw failure;
        }
    }

    private void requireStudent() {
        data.users().findById(studentId).filter(u -> u.role() == Role.STUDENT && u.isActive())
                .orElseThrow(() -> new SecurityException("An active student account is required. Sign out."));
    }

    private static boolean activeTutor(User user) {
        return user != null && user.role() == Role.TUTOR && user.isActive();
    }

    private static boolean activeModule(Module module) {
        return module != null && module.isActive();
    }
}
