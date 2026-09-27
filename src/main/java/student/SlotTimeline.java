package student;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ui.AppUi;

/** A horizontal time grid with separate lanes for overlapping consultation choices. */
final class SlotTimeline extends Pane {
    static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
            .withZone(SINGAPORE);
    private static final double LABEL_WIDTH = 176;
    private static final double HOUR_WIDTH = 112;
    private static final double HEADER_HEIGHT = 40;
    private static final double ROW_HEIGHT = 84;
    private final List<Placement> placements = new ArrayList<>();

    SlotTimeline(LocalDate date, List<StudentService.SlotRow> slots, Consumer<StudentService.SlotRow> select) {
        setId("student-slots");
        getStyleClass().add("slot-timeline");
        var lanes = lanes(slots);
        double midnight = date.atStartOfDay(SINGAPORE).toEpochSecond();
        int firstHour = Math.min(8, (int) Math.floor(slots.stream().mapToDouble(s ->
                (s.start().getEpochSecond() - midnight) / 3600).min().orElse(8)));
        int lastHour = Math.max(18, (int) Math.ceil(slots.stream().mapToDouble(s ->
                (s.end().getEpochSecond() - midnight) / 3600).max().orElse(18)));
        double width = LABEL_WIDTH + (lastHour - firstHour) * HOUR_WIDTH + HOUR_WIDTH;
        double height = HEADER_HEIGHT + lanes.size() * ROW_HEIGHT;
        setMinSize(width, height);
        setPrefSize(width, height);
        add(AppUi.label("COURSE / TUTOR", "timeline-axis"), 12, 0, LABEL_WIDTH - 12, HEADER_HEIGHT);
        for (int hour = firstHour; hour <= lastHour; hour++) {
            double x = LABEL_WIDTH + (hour - firstHour) * HOUR_WIDTH;
            Label time = AppUi.label(String.format(Locale.ENGLISH, "%02d:00%s", hour % 24,
                    hour >= 24 ? " +" + hour / 24 + "d" : ""), "timeline-axis");
            add(time, x, 0, HOUR_WIDTH, HEADER_HEIGHT);
            Region line = new Region();
            line.getStyleClass().add("timeline-line");
            add(line, x, HEADER_HEIGHT, 1, height - HEADER_HEIGHT);
        }
        ToggleGroup choices = new ToggleGroup();
        for (int index = 0; index < lanes.size(); index++) {
            List<StudentService.SlotRow> lane = lanes.get(index);
            var first = lane.getFirst();
            double y = HEADER_HEIGHT + index * ROW_HEIGHT;
            VBox label = new VBox(5, AppUi.label(first.module(), "timeline-course"), AppUi.label(first.tutor(), "muted"));
            label.setAlignment(Pos.CENTER_LEFT);
            label.getStyleClass().add("course-colour-" + colour(first.module()));
            add(label, 12, y, LABEL_WIDTH - 24, ROW_HEIGHT);
            Region separator = new Region();
            separator.getStyleClass().add("timeline-line");
            add(separator, 0, y + ROW_HEIGHT - 1, width, 1);
            for (StudentService.SlotRow slot : lane) {
                double start = (slot.start().getEpochSecond() - midnight) / 3600;
                double duration = (slot.end().getEpochSecond() - slot.start().getEpochSecond()) / 3600.0;
                ToggleButton block = new ToggleButton(TIME.format(slot.start()) + "\n" + TIME.format(slot.end()));
                block.setId("student-slot-" + slot.id());
                block.getStyleClass().addAll("slot-block", "course-colour-" + colour(slot.module()));
                block.setToggleGroup(choices);
                String description = slot.module() + " · " + slot.tutor() + " · "
                        + TIME.format(slot.start()) + "–" + TIME.format(slot.end()) + " SGT";
                block.setAccessibleText(description);
                block.setTooltip(new Tooltip(description));
                block.setOnAction(event -> {
                    block.setSelected(true);
                    select.accept(slot);
                });
                add(block, LABEL_WIDTH + (start - firstHour) * HOUR_WIDTH + 1, y + 8,
                        Math.max(2, duration * HOUR_WIDTH - 2), ROW_HEIGHT - 16);
            }
        }
    }

    static int colour(String module) { return Math.floorMod(module.hashCode(), 5); }

    /** Greedy interval partitioning keeps every overlapping option independently selectable. */
    static List<List<StudentService.SlotRow>> lanes(List<StudentService.SlotRow> slots) {
        List<List<StudentService.SlotRow>> lanes = new ArrayList<>();
        for (var slot : slots.stream().sorted(Comparator.comparing(StudentService.SlotRow::module)
                .thenComparing(StudentService.SlotRow::tutor).thenComparing(StudentService.SlotRow::start)
                .thenComparing(StudentService.SlotRow::id)).toList()) {
            var matching = lanes.stream().filter(lane -> lane.getFirst().module().equals(slot.module())
                    && lane.getFirst().tutor().equals(slot.tutor())
                    && !lane.getLast().end().isAfter(slot.start())).findFirst();
            if (matching.isPresent()) { matching.get().add(slot); }
            else { lanes.add(new ArrayList<>(List.of(slot))); }
        }
        return lanes;
    }

    private void add(Region region, double x, double y, double width, double height) {
        region.setMinSize(0, 0);
        placements.add(new Placement(region, x, y, width, height));
        getChildren().add(region);
    }

    @Override protected void layoutChildren() {
        for (Placement p : placements) { p.region().resizeRelocate(p.x(), p.y(), p.width(), p.height()); }
    }

    private record Placement(Region region, double x, double y, double width, double height) { }
}
