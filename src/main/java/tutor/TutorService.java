package tutor;

import data.repository.Repositories;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import model.consultation.ConsultationSlot;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationNote;
import model.consultation.SlotStatus;
import model.module.Module;
import model.module.TutorModule;
import model.user.Student;
import model.user.Tutor;
import util.OperationLog;

/** Tutor-role business rules independent of the JavaFX user interface. */
public final class TutorService {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private final Repositories data;
    private final UUID actorId;
    private final Clock clock;
    private final OperationLog log;

    public TutorService(Repositories data, UUID actorId, Clock clock, OperationLog log) {
        this.data = Objects.requireNonNull(data, "data");
        this.actorId = Objects.requireNonNull(actorId, "actorId");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.log = Objects.requireNonNull(log, "log");
    }

    public ConsultationSlot createSlot(UUID moduleId, Instant startTime, Instant endTime) {
        UUID slotId = UUID.randomUUID();
        return execute("tutor.slot.create", slotId, () -> createSlot(slotId, moduleId, startTime, endTime));
    }

    public ConsultationSlot cancelSlot(UUID slotId) {
        UUID validatedSlotId = Objects.requireNonNull(slotId, "slotId");
        return execute("tutor.slot.cancel", validatedSlotId, () -> {
            Tutor tutor = requireActiveTutor();
            ConsultationSlot slot = data.slots().findById(validatedSlotId)
                    .orElseThrow(() -> new IllegalArgumentException("Slot does not exist"));
            if (!slot.tutorId().equals(tutor.id()) || slot.status() != SlotStatus.AVAILABLE) {
                throw new IllegalArgumentException("Only an owned available slot can be cancelled");
            }
            return data.slots().save(slot.withStatus(SlotStatus.CANCELLED));
        });
    }

    public List<ConsultationSlot> findUpcomingSlots(LocalDate date) {
        LocalDate selectedDate = Objects.requireNonNull(date, "date");
        return execute("tutor.slot.upcoming", null, () -> {
            Tutor tutor = requireActiveTutor();
            return data.slots().findByTutorId(tutor.id()).stream()
                    .filter(slot -> !slot.startTime().isBefore(clock.instant()))
                    .filter(slot -> slot.status() == SlotStatus.AVAILABLE || slot.status() == SlotStatus.BOOKED)
                    .filter(slot -> slot.startTime().atZone(SINGAPORE).toLocalDate().equals(selectedDate))
                    .sorted(Comparator.comparing(ConsultationSlot::startTime).thenComparing(ConsultationSlot::id))
                    .toList();
        });
    }

    public List<ConsultationSlot> findSlotHistory(LocalDate date, SlotStatus status) {
        LocalDate selectedDate = Objects.requireNonNull(date, "date");
        SlotStatus selectedStatus = Objects.requireNonNull(status, "status");
        return execute("tutor.slot.history", null, () -> {
            Tutor tutor = requireTutor();
            if (selectedStatus != SlotStatus.CANCELLED && selectedStatus != SlotStatus.COMPLETED) {
                throw new IllegalArgumentException("Slot history must be cancelled or completed");
            }
            if (!tutor.isActive() && selectedStatus != SlotStatus.COMPLETED) {
                throw new SecurityException("An inactive tutor may only view completed slot history");
            }
            return data.slots().findByTutorId(tutor.id()).stream()
                    .filter(slot -> slot.status() == selectedStatus)
                    .filter(slot -> slot.startTime().atZone(SINGAPORE).toLocalDate().equals(selectedDate))
                    .sorted(Comparator.comparing(ConsultationSlot::startTime).thenComparing(ConsultationSlot::id))
                    .toList();
        });
    }

    public List<Module> findActiveAssignedModules() {
        return execute("tutor.module.active", null, () -> {
            Tutor tutor = requireActiveTutor();
            return data.modules().findAssignmentsByTutor(tutor.id()).stream()
                    .map(TutorModule::moduleId)
                    .map(data.modules()::findById)
                    .flatMap(Optional::stream)
                    .filter(Module::isActive)
                    .sorted(Comparator.comparing(Module::code).thenComparing(Module::id))
                    .toList();
        });
    }

    public List<Booking> findBookings(BookingFilter filter) {
        BookingFilter selectedFilter = Objects.requireNonNull(filter, "filter");
        return execute("tutor.booking.find", null, () -> {
            Tutor tutor = requireTutor();
            return findBookings(tutor, selectedFilter);
        });
    }

    public List<TutorBookingView> findBookingViews(BookingFilter filter) {
        BookingFilter selectedFilter = Objects.requireNonNull(filter, "filter");
        return execute("tutor.booking.view", null, () -> {
            Tutor tutor = requireTutor();
            return findBookings(tutor, selectedFilter).stream()
                    .map(this::toBookingView)
                    .toList();
        });
    }

    public Booking completeBooking(UUID bookingId) {
        UUID validatedBookingId = Objects.requireNonNull(bookingId, "bookingId");
        return execute("tutor.booking.complete", validatedBookingId, () -> {
            requireActiveTutor();
            return data.lifecycle().completeActiveBooking(actorId, validatedBookingId);
        });
    }

    public ConsultationNote saveNote(UUID bookingId, String content) {
        UUID validatedBookingId = Objects.requireNonNull(bookingId, "bookingId");
        return execute("tutor.note.save", validatedBookingId, () -> {
            Tutor tutor = requireActiveTutor();
            Booking booking = data.bookings().findById(validatedBookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
            ConsultationSlot slot = data.slots().findById(booking.slotId())
                    .orElseThrow(() -> new IllegalArgumentException("Slot does not exist"));
            if (!slot.tutorId().equals(tutor.id())) {
                throw new IllegalArgumentException("Booking does not belong to tutor");
            }
            return data.bookings().saveNote(new ConsultationNote(validatedBookingId, content, clock.instant()));
        });
    }

    public Optional<ConsultationNote> findNote(UUID bookingId) {
        UUID validatedBookingId = Objects.requireNonNull(bookingId, "bookingId");
        return execute("tutor.note.find", validatedBookingId, () -> {
            Tutor tutor = requireTutor();
            Booking booking = data.bookings().findById(validatedBookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
            ConsultationSlot slot = data.slots().findById(booking.slotId())
                    .orElseThrow(() -> new IllegalArgumentException("Slot does not exist"));
            if (!slot.tutorId().equals(tutor.id())) {
                throw new IllegalArgumentException("Booking does not belong to tutor");
            }
            return data.bookings().findNoteByBookingId(validatedBookingId);
        });
    }

    private ConsultationSlot createSlot(UUID slotId, UUID moduleId, Instant startTime, Instant endTime) {
        Tutor tutor = requireActiveTutor();
        Module module = data.modules().findById(Objects.requireNonNull(moduleId, "moduleId"))
                .orElseThrow(() -> new IllegalArgumentException("Module does not exist"));
        if (!module.isActive()) {
            throw new IllegalArgumentException("Module is inactive");
        }
        if (!data.modules().findAssignmentsByTutor(tutor.id())
                .contains(new TutorModule(tutor.id(), module.id()))) {
            throw new IllegalArgumentException("Tutor is not assigned to module");
        }
        if (Objects.requireNonNull(startTime, "startTime").isBefore(clock.instant())) {
            throw new IllegalArgumentException("Slot cannot start in the past");
        }
        Instant validatedEndTime = Objects.requireNonNull(endTime, "endTime");
        boolean overlapsAvailableSlot = data.slots().findByTutorId(tutor.id()).stream()
                .filter(slot -> slot.status() == SlotStatus.AVAILABLE || slot.status() == SlotStatus.BOOKED)
                .anyMatch(slot -> startTime.isBefore(slot.endTime()) && validatedEndTime.isAfter(slot.startTime()));
        if (overlapsAvailableSlot) {
            throw new IllegalArgumentException("Slot overlaps an existing slot");
        }
        ConsultationSlot slot = new ConsultationSlot(slotId, tutor.id(), module.id(),
                startTime, validatedEndTime, SlotStatus.AVAILABLE);
        return data.slots().save(slot);
    }

    private Tutor requireActiveTutor() {
        Tutor tutor = requireTutor();
        if (!tutor.isActive()) {
            throw new SecurityException("An active tutor account is required. Sign out.");
        }
        return tutor;
    }

    private List<Booking> findBookings(Tutor tutor, BookingFilter filter) {
        if (!tutor.isActive() && filter.status() != BookingStatus.COMPLETED) {
            throw new SecurityException("An inactive tutor may only view completed booking history");
        }
        return data.bookings().findAll().stream()
                .filter(booking -> data.slots().findById(booking.slotId())
                        .filter(slot -> slot.tutorId().equals(tutor.id())).isPresent())
                .filter(booking -> filter.moduleId() == null || data.slots().findById(booking.slotId())
                        .filter(slot -> slot.moduleId().equals(filter.moduleId())).isPresent())
                .filter(booking -> filter.date() == null || data.slots().findById(booking.slotId())
                        .filter(slot -> slot.startTime().atZone(SINGAPORE).toLocalDate().equals(filter.date()))
                        .isPresent())
                .filter(booking -> filter.status() == null || booking.status() == filter.status())
                .sorted(Comparator.comparing((Booking booking) -> data.slots().findById(booking.slotId())
                        .orElseThrow().startTime()).thenComparing(Booking::id))
                .toList();
    }

    private TutorBookingView toBookingView(Booking booking) {
        ConsultationSlot slot = data.slots().findById(booking.slotId())
                .orElseThrow(() -> new IllegalArgumentException("Slot does not exist"));
        Student student = data.users().findById(booking.studentId())
                .filter(Student.class::isInstance)
                .map(Student.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Student does not exist"));
        Module module = data.modules().findById(slot.moduleId())
                .orElseThrow(() -> new IllegalArgumentException("Module does not exist"));
        return new TutorBookingView(booking.id(), slot.id(), student.name(), module.code(),
                slot.startTime(), slot.endTime(), booking.status());
    }

    private Tutor requireTutor() {
        return data.users().findById(actorId)
                .filter(Tutor.class::isInstance)
                .map(Tutor.class::cast)
                .orElseThrow(() -> new SecurityException("A tutor account is required. Sign out."));
    }

    private <T> T execute(String operation, UUID entityId, Supplier<T> action) {
        T result;
        try {
            result = data.lifecycle().withExclusiveAccess(action);
        } catch (RuntimeException failure) {
            log.record(operation, actorId, entityId, failure);
            throw failure;
        }
        log.record(operation, actorId, entityId, null);
        return result;
    }
}
