package student;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.scene.layout.VBox;
import model.consultation.BookingStatus;
import ui.AppUi;

final class StudentWorkspace {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm")
            .withLocale(Locale.ENGLISH).withZone(ZoneId.of("Asia/Singapore"));
    private final StudentService service;
    private final Predicate<String> confirm;
    private final BorderPane root = new BorderPane();
    private final StudentSlotBrowser slotBrowser;
    private final TableView<StudentService.BookingRow> bookings = table("student-bookings");
    private final TextField bookingCourseSearch = new TextField();
    private final TextField bookingTutorSearch = new TextField();
    private final DatePicker bookingDate = new DatePicker();
    private final Label message = new Label();

    StudentWorkspace(StudentService service, Runnable signOut, Predicate<String> confirm) {
        this.service = service;
        this.confirm = confirm;
        slotBrowser = new StudentSlotBrowser(service::today, () -> refresh(""),
                id -> act(() -> service.book(id)));
        Label heading = new Label("Student workspace");
        heading.setId("role-heading");
        Button refresh = button("Refresh", "student-refresh", () -> refresh("Refreshed"));
        Button logout = button("Sign out", "sign-out", signOut);
        TabPane tabs = new TabPane(slotsTab(), bookingsTab());
        tabs.setId("student-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        message.setId("student-message");
        AppUi.workspace(root, heading, "A little guidance. A clearer way forward.", refresh, logout, tabs, message);
        refresh("");
    }

    Parent root() { return root; }

    private Tab slotsTab() {
        return new Tab("Available slots", slotBrowser.root());
    }

    private Tab bookingsTab() {
        column(bookings, "Module", StudentService.BookingRow::module);
        column(bookings, "Tutor", StudentService.BookingRow::tutor);
        column(bookings, "Start (SGT)", b -> b.start() == null ? "Unavailable" : TIME.format(b.start()));
        column(bookings, "End (SGT)", b -> b.end() == null ? "Unavailable" : TIME.format(b.end()));
        column(bookings, "Status", b -> b.status().toString());
        Button cancel = button("Cancel selected", "student-cancel", this::cancelSelected);
        bookingCourseSearch.setId("student-booking-course");
        bookingCourseSearch.setPromptText("Course code or name");
        bookingTutorSearch.setId("student-booking-tutor");
        bookingTutorSearch.setPromptText("Tutor name");
        bookingDate.setId("student-booking-date");
        bookingDate.setPromptText("Date (SGT)");
        Button search = button("Search", "student-booking-search", () -> refresh("Filtered"));
        Button clear = button("Clear", "student-booking-clear", () -> {
            bookingCourseSearch.clear();
            bookingTutorSearch.clear();
            bookingDate.setValue(null);
            refresh("Filters cleared");
        });
        bookingCourseSearch.setOnAction(event -> search.fire());
        bookingTutorSearch.setOnAction(event -> search.fire());
        AppUi.danger(cancel);
        bookings.setPlaceholder(AppUi.empty("No bookings to show", "Clear your filters or book a consultation in Available slots."));
        VBox layout = AppUi.section("Your time, organised", "Review upcoming consultations and your booking history.",
                AppUi.filters(AppUi.field("Course", bookingCourseSearch), AppUi.field("Tutor", bookingTutorSearch),
                        AppUi.field("Date · SGT", bookingDate), search, clear), bookings, cancel);
        return new Tab("My bookings", layout);
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
            StudentService.Snapshot snapshot = service.load(
                    slotBrowser.filter(),
                    new BookingFilter(bookingCourseSearch.getText(), bookingTutorSearch.getText(),
                            bookingDate.getValue()));
            slotBrowser.update(snapshot.available());
            bookings.getItems().setAll(snapshot.bookings());
            message.setText(success);
        } catch (RuntimeException failure) {
            slotBrowser.update(java.util.List.of());
            if (failure instanceof SecurityException) { bookings.getItems().clear(); }
            showFailure(failure);
        }
    }

    private void act(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            refresh("");
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

    private static Button button(String heading, String id, Runnable action) {
        Button button = new Button(heading);
        button.setId(id);
        button.setOnAction(event -> action.run());
        return button;
    }
}
