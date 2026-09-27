package student;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.consultation.BookingStatus;

final class StudentWorkspace {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm")
            .withLocale(Locale.ENGLISH).withZone(ZoneId.of("Asia/Singapore"));
    private final StudentService service;
    private final Predicate<String> confirm;
    private final BorderPane root = new BorderPane();
    private final TableView<StudentService.SlotRow> slots = table("student-slots");
    private final TextField moduleSearch = new TextField();
    private final TextField tutorSearch = new TextField();
    private final DatePicker slotDate = new DatePicker();
    private final TableView<StudentService.BookingRow> bookings = table("student-bookings");
    private final Label message = new Label();

    StudentWorkspace(StudentService service, Runnable signOut, Predicate<String> confirm) {
        this.service = service;
        this.confirm = confirm;
        Label heading = new Label("Student workspace");
        heading.setId("role-heading");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Button refresh = button("Refresh", "student-refresh", () -> refresh("Refreshed"));
        Button logout = button("Sign out", "sign-out", signOut);
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        root.setTop(new HBox(12, heading, space, refresh, logout));
        TabPane tabs = new TabPane(slotsTab(), bookingsTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setCenter(tabs);
        message.setId("student-message");
        message.setWrapText(true);
        root.setBottom(message);
        root.setPadding(new Insets(18));
        BorderPane.setMargin(tabs, new Insets(16, 0, 12, 0));
        refresh("");
    }

    Parent root() { return root; }

    private Tab slotsTab() {
        column(slots, "Module", StudentService.SlotRow::module);
        column(slots, "Tutor", StudentService.SlotRow::tutor);
        column(slots, "Start (SGT)", s -> TIME.format(s.start()));
        column(slots, "End (SGT)", s -> TIME.format(s.end()));
        Button book = button("Book selected", "student-book", () -> act(() ->
                service.book(selected(slots).id())));
        moduleSearch.setId("student-slot-module");
        moduleSearch.setPromptText("Module code or name");
        tutorSearch.setId("student-slot-tutor");
        tutorSearch.setPromptText("Tutor name");
        slotDate.setId("student-slot-date");
        slotDate.setPromptText("Date (SGT)");
        Button search = button("Search", "student-slot-search", () -> refresh("Filtered"));
        Button clear = button("Clear", "student-slot-clear", () -> {
            moduleSearch.clear();
            tutorSearch.clear();
            slotDate.setValue(null);
            refresh("Filters cleared");
        });
        VBox layout = new VBox(10, new FlowPane(8, 8, moduleSearch, tutorSearch, slotDate, search, clear),
                book, slots);
        layout.setPadding(new Insets(10, 0, 0, 0));
        VBox.setVgrow(slots, Priority.ALWAYS);
        return new Tab("Available slots", layout);
    }

    private Tab bookingsTab() {
        column(bookings, "Module", StudentService.BookingRow::module);
        column(bookings, "Tutor", StudentService.BookingRow::tutor);
        column(bookings, "Start (SGT)", b -> b.start() == null ? "Unavailable" : TIME.format(b.start()));
        column(bookings, "End (SGT)", b -> b.end() == null ? "Unavailable" : TIME.format(b.end()));
        column(bookings, "Status", b -> b.status().toString());
        Button cancel = button("Cancel selected", "student-cancel", this::cancelSelected);
        return tab("My bookings", bookings, cancel);
    }

    private void cancelSelected() {
        StudentService.BookingRow chosen;
        try {
            chosen = selected(bookings);
            if (chosen.status() != BookingStatus.ACTIVE) {
                throw new IllegalArgumentException("Select an active booking");
            }
        } catch (IllegalArgumentException failure) {
            showFailure(failure);
            return;
        }
        if (confirm.test("Cancel the booking for " + chosen.module() + " at "
                + (chosen.start() == null ? "an unavailable time" : TIME.format(chosen.start())) + " SGT?")) {
            act(() -> service.cancel(chosen.id()));
        }
    }

    private void refresh(String success) {
        try {
            StudentService.Snapshot snapshot = service.load(new SlotFilter(
                    moduleSearch.getText(), tutorSearch.getText(), slotDate.getValue()));
            slots.getItems().setAll(snapshot.available());
            bookings.getItems().setAll(snapshot.bookings());
            message.setText(success);
        } catch (RuntimeException failure) {
            if (failure instanceof SecurityException) { slots.getItems().clear(); bookings.getItems().clear(); }
            showFailure(failure);
        }
    }

    private void act(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            if (failure instanceof SecurityException) { slots.getItems().clear(); bookings.getItems().clear(); }
            showFailure(failure);
            return;
        }
        refresh("Done");
    }

    private void showFailure(RuntimeException failure) {
        message.setText(failure instanceof IllegalArgumentException || failure instanceof SecurityException
                ? failure.getMessage() : "Storage operation failed. Refresh and try again.");
    }

    private static <T> T selected(TableView<T> table) {
        T chosen = table.getSelectionModel().getSelectedItem();
        if (chosen == null) { throw new IllegalArgumentException("Select a record first"); }
        return chosen;
    }

    private static <T> TableView<T> table(String id) {
        TableView<T> table = new TableView<>();
        table.setId(id);
        table.setPlaceholder(new Label("No records"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private static <T> void column(TableView<T> table, String heading, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(heading);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(170);
        table.getColumns().add(column);
    }

    private static Tab tab(String heading, TableView<?> table, Button action) {
        VBox layout = new VBox(10, action, table);
        layout.setPadding(new Insets(10, 0, 0, 0));
        VBox.setVgrow(table, Priority.ALWAYS);
        return new Tab(heading, layout);
    }

    private static Button button(String heading, String id, Runnable action) {
        Button button = new Button(heading);
        button.setId(id);
        button.setOnAction(event -> action.run());
        return button;
    }
}
