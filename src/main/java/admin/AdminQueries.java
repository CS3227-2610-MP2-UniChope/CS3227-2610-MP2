package admin;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.module.Module;
import model.user.User;

/** Pure joins/aggregations, separate from authorization and JavaFX. */
public final class AdminQueries {
    private AdminQueries() { }

    public record BookingRow(UUID id, String student, String tutor, String module,
                             Instant start, Instant end, BookingStatus status,
                             UUID tutorId, UUID moduleId) { }
    public record AssignmentRow(UUID tutorId, UUID moduleId, String tutor, String module) { }
    public record CountRow(String label, long count) { }
    public record Statistics(long total, long completed, long cancelled,
                             List<CountRow> byModule, List<CountRow> byTutor) {
        public double completionRate() { return total == 0 ? 0 : 100.0 * completed / total; }
        public double cancellationRate() { return total == 0 ? 0 : 100.0 * cancelled / total; }
    }

    public static List<BookingRow> bookings(AdminSnapshot data) {
        var users = index(data.users(), User::id);
        var modules = index(data.modules(), Module::id);
        var slots = index(data.slots(), ConsultationSlot::id);
        return data.bookings().stream().map(b -> {
            ConsultationSlot slot = slots.get(b.slotId());
            return new BookingRow(b.id(), userLabel(users.get(b.studentId())),
                    userLabel(slot == null ? null : users.get(slot.tutorId())),
                    moduleLabel(slot == null ? null : modules.get(slot.moduleId())),
                    slot == null ? null : slot.startTime(), slot == null ? null : slot.endTime(),
                    b.status(), slot == null || !users.containsKey(slot.tutorId()) ? null : slot.tutorId(),
                    slot == null || !modules.containsKey(slot.moduleId()) ? null : slot.moduleId());
        }).sorted(Comparator.comparing(BookingRow::start, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BookingRow::id)).toList();
    }

    public static List<AssignmentRow> assignments(AdminSnapshot data) {
        var users = index(data.users(), User::id);
        var modules = index(data.modules(), Module::id);
        return data.assignments().stream().map(a -> new AssignmentRow(a.tutorId(), a.moduleId(),
                userLabel(users.get(a.tutorId())), moduleLabel(modules.get(a.moduleId()))))
                .sorted(Comparator.comparing(AssignmentRow::module).thenComparing(AssignmentRow::tutor)
                        .thenComparing(AssignmentRow::moduleId).thenComparing(AssignmentRow::tutorId)).toList();
    }

    public static Statistics statistics(AdminSnapshot data) {
        var rows = bookings(data);
        return new Statistics(rows.size(), rows.stream().filter(b -> b.status() == BookingStatus.COMPLETED).count(),
                rows.stream().filter(b -> b.status() == BookingStatus.CANCELLED).count(),
                counts(rows, data, true), counts(rows, data, false));
    }

    private static List<CountRow> counts(List<BookingRow> rows, AdminSnapshot data, boolean module) {
        record Group(UUID id, String label) { }
        var users = index(data.users(), User::id);
        return rows.stream().collect(Collectors.groupingBy(row -> {
            UUID id = module ? row.moduleId() : row.tutorId();
            String label = "Unknown";
            if (id != null) {
                label = module ? row.module() : row.tutor() + " (" + users.get(id).email() + ")";
            }
            // Identity is the grouping key; names/emails are only display labels.
            return new Group(id, label);
        }, Collectors.counting())).entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<Group, Long> e) -> e.getKey().label())
                        .thenComparing(e -> e.getKey().id(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(e -> new CountRow(e.getKey().label(), e.getValue())).toList();
    }

    static String userLabel(User user) {
        return user == null ? "Unavailable" : user.name() + (user.isActive() ? "" : " (inactive)");
    }

    static String moduleLabel(Module module) {
        return module == null ? "Unavailable" : module.code() + " - " + module.name()
                + (module.isActive() ? "" : " (inactive)");
    }

    private static <T> Map<UUID, T> index(List<T> values, Function<T, UUID> key) {
        return values.stream().collect(Collectors.toMap(key, Function.identity()));
    }
}
