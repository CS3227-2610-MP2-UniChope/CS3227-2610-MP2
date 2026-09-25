package admin;

import data.memory.InMemoryRepositories;
import data.repository.Repositories;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import model.consultation.*;
import model.module.Module;
import model.user.*;
import util.OperationLog;

final class AdminFixture {
    final Repositories data = InMemoryRepositories.create();
    final Clock clock = Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"), ZoneOffset.UTC);
    final List<OperationLog.Event> events = new ArrayList<>();
    final Admin actor = new Admin(UUID.randomUUID(), "Admin", "admin@example.edu", true);
    final Student student = new Student(UUID.randomUUID(), "Student", "student@example.edu", true);
    final Tutor tutor = new Tutor(UUID.randomUUID(), "Tutor", "tutor@example.edu", true);
    final Module module = new Module(UUID.randomUUID(), "CS3227", "Software Engineering", true);
    final AdminService service;

    AdminFixture() {
        data.users().save(actor); data.users().save(student); data.users().save(tutor);
        data.modules().save(module);
        service = new AdminService(data, actor.id(), clock, new OperationLog(clock, events::add));
    }

    ConsultationSlot slot(Instant start, SlotStatus status) {
        return data.slots().save(new ConsultationSlot(UUID.randomUUID(), tutor.id(), module.id(),
                start, start.plusSeconds(1800), status));
    }

    Booking booking(ConsultationSlot slot, BookingStatus status) {
        return data.bookings().save(new Booking(UUID.randomUUID(), student.id(), slot.id(), clock.instant(), status));
    }
}
