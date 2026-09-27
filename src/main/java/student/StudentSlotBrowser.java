package student;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import ui.AppUi;

/** Calendar first, then a colour-coded day timetable and an explicit booking action. */
final class StudentSlotBrowser {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE, d MMM uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH)
            .withZone(SlotTimeline.SINGAPORE);
    private final Supplier<LocalDate> today;
    private final Runnable refresh;
    private final Consumer<UUID> book;
    private final VBox root = new VBox(14);
    private final TextField course = new TextField();
    private final TextField tutor = new TextField();
    private List<StudentService.SlotRow> available = List.of();
    private YearMonth month;
    private LocalDate selectedDate;

    StudentSlotBrowser(Supplier<LocalDate> today, Runnable refresh, Consumer<UUID> book) {
        this.today = today;
        this.refresh = refresh;
        this.book = book;
        month = YearMonth.from(today.get());
        root.getStyleClass().addAll("content-section", "slot-browser");
        root.setId("student-slot-browser");
        course.setId("student-slot-module");
        course.setPromptText("Code or course name");
        tutor.setId("student-slot-tutor");
        tutor.setPromptText("Tutor name");
        course.setOnAction(event -> refresh.run());
        tutor.setOnAction(event -> refresh.run());
        render();
    }

    VBox root() { return root; }
    SlotFilter filter() { return new SlotFilter(course.getText(), tutor.getText(), null); }

    void update(List<StudentService.SlotRow> slots) {
        available = List.copyOf(slots);
        render();
    }

    private void render() {
        root.getChildren().clear();
        HBox steps = new HBox(22, AppUi.label("01  Choose a date", selectedDate == null ? "planner-step-current" : "muted"),
                AppUi.label("02  Choose a slot", selectedDate == null ? "muted" : "planner-step-current"));
        root.getChildren().add(steps);
        if (selectedDate == null) { calendar(); } else { timetable(); }
    }

    private void calendar() {
        Label monthTitle = AppUi.label(MONTH.format(month), "section-title");
        monthTitle.setId("student-calendar-month");
        Button previous = button("‹", "student-calendar-previous", () -> moveMonth(-1));
        previous.setAccessibleText("Previous month");
        previous.setDisable(!month.isAfter(YearMonth.from(today.get())));
        Button next = button("›", "student-calendar-next", () -> moveMonth(1));
        next.setAccessibleText("Next month");
        Button current = button("This month", "student-calendar-today", () -> {
            month = YearMonth.from(today.get());
            refresh.run();
        });
        root.getChildren().add(row(monthTitle, previous, current, next));
        root.getChildren().add(AppUi.label("Pick a day to see consultation times. Availability and dates are shown in SGT.", "muted"));
        var counts = available.stream().collect(Collectors.groupingBy(s -> s.start().atZone(SlotTimeline.SINGAPORE).toLocalDate(),
                Collectors.counting()));
        GridPane grid = new GridPane();
        grid.setId("student-calendar");
        grid.getStyleClass().add("month-grid");
        for (int column = 0; column < 7; column++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / 7);
            grid.getColumnConstraints().add(constraint);
            Label weekday = AppUi.label(List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").get(column), "calendar-weekday");
            weekday.setMaxWidth(Double.MAX_VALUE);
            grid.add(weekday, column, 0);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        int weeks = (offset + month.lengthOfMonth() + 6) / 7;
        RowConstraints heading = new RowConstraints(30);
        grid.getRowConstraints().add(heading);
        for (int row = 0; row < weeks; row++) {
            RowConstraints constraint = new RowConstraints(62, 88, Double.MAX_VALUE);
            constraint.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(constraint);
        }
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            long count = counts.getOrDefault(date, 0L);
            Button cell = button("", "student-date-" + date, () -> {
                selectedDate = date;
                refresh.run();
            });
            cell.getStyleClass().add("calendar-day");
            if (count > 0) { cell.getStyleClass().add("has-slots"); }
            if (date.equals(today.get())) { cell.getStyleClass().add("calendar-today"); }
            cell.setDisable(date.isBefore(today.get()));
            cell.setAccessibleText(DAY.format(date) + ", " + count + " available slots");
            Label number = AppUi.label(Integer.toString(day), "calendar-number");
            Label availability = AppUi.label(count == 0 ? "—" : count + (count == 1 ? " slot" : " slots"), "calendar-availability");
            cell.setGraphic(new VBox(5, number, availability));
            cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            grid.add(cell, (offset + day - 1) % 7, (offset + day - 1) / 7 + 1);
        }
        ScrollPane calendar = scroll(grid);
        calendar.setFitToHeight(true);
        VBox.setVgrow(calendar, Priority.ALWAYS);
        root.getChildren().add(calendar);
    }

    private void moveMonth(int delta) {
        month = month.plusMonths(delta);
        refresh.run();
    }

    private void timetable() {
        Button back = button("‹ Calendar", "student-back-calendar", () -> {
            month = YearMonth.from(selectedDate);
            selectedDate = null;
            course.clear();
            tutor.clear();
            refresh.run();
        });
        Label date = AppUi.label(DAY.format(selectedDate), "section-title");
        date.setId("student-selected-date");
        Button previous = button("‹", "student-day-previous", () -> moveDay(-1));
        previous.setAccessibleText("Previous day");
        previous.setDisable(!selectedDate.isAfter(today.get()));
        Button next = button("›", "student-day-next", () -> moveDay(1));
        next.setAccessibleText("Next day");
        root.getChildren().add(row(new HBox(14, back, date), previous, next));
        Button search = button("Search", "student-slot-search", refresh);
        AppUi.primary(search);
        Button clear = button("Clear", "student-slot-clear", () -> {
            course.clear();
            tutor.clear();
            refresh.run();
        });
        root.getChildren().add(AppUi.filters(AppUi.field("Course", course), AppUi.field("Tutor", tutor), search, clear));
        List<StudentService.SlotRow> slots = available.stream().filter(s -> s.start().atZone(SlotTimeline.SINGAPORE)
                .toLocalDate().equals(selectedDate)).toList();
        Label count = AppUi.label(slots.size() + (slots.size() == 1 ? " available slot" : " available slots")
                + " · Select a coloured block · All times SGT", "muted");
        count.setId("student-day-count");
        root.getChildren().add(count);
        Label selection = AppUi.label("Select a slot to review its course, tutor, and time.", "muted");
        selection.setId("student-slot-selection");
        selection.setMinHeight(Region.USE_PREF_SIZE);
        Button reserve = button("Book selected", "student-book", () -> { });
        reserve.setDisable(true);
        AppUi.primary(reserve);
        Node schedule = slots.isEmpty() ? AppUi.empty("No matching slots on this day", "Try another date or clear your course and tutor filters.")
                : new SlotTimeline(selectedDate, slots, slot -> {
                    selection.setText(slot.module() + " · " + slot.tutor() + "\n"
                            + TIME.format(slot.start()) + " – " + TIME.format(slot.end()) + " SGT");
                    reserve.setDisable(false);
                    reserve.setOnAction(event -> book.accept(slot.id()));
                });
        ScrollPane timeline = scroll(schedule);
        VBox.setVgrow(timeline, Priority.ALWAYS);
        HBox bookingBar = row(selection, reserve);
        bookingBar.setMinHeight(Region.USE_PREF_SIZE);
        root.getChildren().addAll(timeline, bookingBar);
    }

    private void moveDay(int delta) {
        selectedDate = selectedDate.plusDays(delta);
        refresh.run();
    }

    private static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("planner-scroll");
        scroll.setFitToWidth(true);
        scroll.setMinHeight(70);
        return scroll;
    }

    private static HBox row(Node leading, Node... actions) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, leading, spacer);
        row.getChildren().addAll(actions);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Button button(String text, String id, Runnable action) {
        Button button = new Button(text);
        button.setId(id);
        button.setOnAction(event -> action.run());
        return button;
    }
}
