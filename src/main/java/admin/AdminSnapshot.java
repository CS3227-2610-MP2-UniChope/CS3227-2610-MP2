package admin;

import java.util.List;
import model.user.User;
import model.module.Module;
import model.module.TutorModule;
import model.consultation.Booking;
import model.consultation.ConsultationSlot;

/** One consistent immutable snapshot for all admin tables and statistics. */
public record AdminSnapshot(List<User> users, List<Module> modules,
                            List<TutorModule> assignments, List<ConsultationSlot> slots,
                            List<Booking> bookings) {
    public AdminSnapshot {
        users = List.copyOf(users);
        modules = List.copyOf(modules);
        assignments = List.copyOf(assignments);
        slots = List.copyOf(slots);
        bookings = List.copyOf(bookings);
    }
}
