package tutor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import model.consultation.ConsultationSlot;
import model.consultation.BookingStatus;
import model.consultation.SlotStatus;
import model.module.Module;
import ui.AppUi;

/** JavaFX presentation for tutor slot management; TutorService owns business rules. */
final class TutorWorkspace {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    private final TutorService service;
    private final BorderPane root = new BorderPane();
    private final Label message = new Label();
    private final DatePicker date = new DatePicker();
    private final ComboBox<Module> modules = new ComboBox<>();
    private final TextField start = field("Start (HH:mm)", "slot-start");
    private final TextField end = field("End (HH:mm)", "slot-end");
    private final TableView<TutorSlotView> slots = table("slot-table");
    private final ComboBox<Module> bookingModules = new ComboBox<>();
    private final DatePicker bookingDate = new DatePicker();
    private final ComboBox<BookingStatus> bookingStatus = new ComboBox<>();
    private final TableView<TutorBookingView> bookings = table("booking-table");
    private final DatePicker historyDate = new DatePicker(LocalDate.now(SINGAPORE));
    private final ComboBox<SlotStatus> historyStatus = new ComboBox<>();
    private final TableView<ConsultationSlot> historySlots = table("history-slot-table");
    private final TableView<TutorBookingView> historyBookings = table("history-booking-table");
    private final TextArea note = new TextArea();

    TutorWorkspace(TutorService service, Runnable signOut) {
        this.service = service;
        Label heading = new Label("Tutor workspace");
        heading.setId("role-heading");
        Button refresh = button("Refresh", "tutor-refresh", () -> refresh("Refreshed"));
        Button logout = button("Sign out", "sign-out", signOut);
        TabPane tabs = new TabPane(slotsTab(), bookingsTab(), historyTab());
        tabs.setId("tutor-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        message.setId("tutor-message");
        AppUi.workspace(root, heading, "Make room for questions. Help the next idea take shape.", refresh, logout, tabs, message);
        refresh("");
    }

    Parent root() { return root; }

    private Tab slotsTab() {
        column(slots, "Module", TutorSlotView::moduleCode);
        column(slots, "Start (SGT)", slot -> displayTime(slot.startTime()));
        column(slots, "End (SGT)", slot -> displayTime(slot.endTime()));
        column(slots, "Status", slot -> slot.status().toString());
        date.setId("slot-date");
        date.setPromptText("Filter date");
        date.valueProperty().addListener((observable, oldDate, newDate) -> refresh(""));
        Button showAllUpcoming = button("Show all upcoming", "show-all-upcoming", () -> {
            if (date.getValue() == null) {
                refresh("");
            } else {
                date.setValue(null);
            }
        });
        modules.setId("slot-module");
        modules.setPromptText("Active assigned module");
        modules.setConverter(converter(Module::code));
        Button create = button("Create", "create-slot", () -> act(() -> {
            Module module = modules.getValue();
            if (module == null) { throw new IllegalArgumentException("Select an active assigned module"); }
            service.createSlot(module.id(), instant(start.getText()), instant(end.getText()));
            start.clear();
            end.clear();
        }));
        Button cancel = button("Cancel selected", "cancel-slot", () -> act(() ->
                service.cancelSlot(selected(slots).slotId())));
        AppUi.primary(create);
        AppUi.danger(cancel);
        return new Tab("Slots", AppUi.section("Make time for your students", "Choose a date and course to offer a consultation. Times shown in SGT.",
                AppUi.filters(AppUi.field("Date · SGT", date), AppUi.field("Assigned course", modules),
                        AppUi.field("Starts · HH:mm", start), AppUi.field("Ends · HH:mm", end), showAllUpcoming), slots, create, cancel));
    }

    private Tab bookingsTab() {
        column(bookings, "Student", TutorBookingView::studentName);
        column(bookings, "Module", TutorBookingView::moduleCode);
        column(bookings, "Start (SGT)", booking -> displayTime(booking.startTime()));
        column(bookings, "End (SGT)", booking -> displayTime(booking.endTime()));
        column(bookings, "Status", booking -> booking.status().toString());
        bookingModules.setId("booking-module");
        bookingModules.setPromptText("Module");
        bookingModules.setConverter(converter(Module::code));
        bookingDate.setId("booking-date");
        bookingDate.setPromptText("Date");
        bookingStatus.setId("booking-status");
        bookingStatus.setPromptText("Status");
        bookingStatus.getItems().setAll(BookingStatus.values());
        Button filter = button("Filter", "filter-bookings", () -> refresh("Filtered"));
        Button complete = button("Complete selected", "complete-booking", () -> act(() ->
                service.completeBooking(selected(bookings).bookingId())));
        AppUi.primary(complete);
        return new Tab("Bookings", AppUi.section("Your consultation schedule", "Find a booking and mark the consultation complete when you are done.",
                AppUi.filters(AppUi.field("Course", bookingModules), AppUi.field("Date · SGT", bookingDate),
                        AppUi.field("Status", bookingStatus), filter), bookings, complete));
    }

    private Tab historyTab() {
        column(historySlots, "Start (SGT)", slot -> displayTime(slot.startTime()));
        column(historySlots, "Status", slot -> slot.status().toString());
        column(historyBookings, "Student", TutorBookingView::studentName);
        column(historyBookings, "Module", TutorBookingView::moduleCode);
        column(historyBookings, "Status", booking -> booking.status().toString());
        historyDate.setId("history-date");
        historyStatus.setId("history-status");
        historyStatus.getItems().setAll(SlotStatus.CANCELLED, SlotStatus.COMPLETED);
        historyStatus.setValue(SlotStatus.COMPLETED);
        note.setId("note-content");
        note.setPromptText("Consultation note");
        Button filter = button("Filter history", "filter-history", () -> refresh("Filtered"));
        Button load = button("Load note", "load-note", () -> act(() -> note.setText(service.findNote(
                selected(historyBookings).bookingId()).map(value -> value.content()).orElse(""))));
        Button save = button("Save note", "save-note", () -> act(() ->
                service.saveNote(selected(historyBookings).bookingId(), note.getText())));
        AppUi.primary(save);
        note.setPrefRowCount(3);
        note.setWrapText(true);
        historySlots.setPlaceholder(new Label("No slots on this date"));
        historyBookings.setPlaceholder(new Label("No completed consultations"));
        historySlots.setMinHeight(80);
        historyBookings.setMinHeight(80);
        historyBookings.setPrefHeight(150);
        note.setMinHeight(60);
        note.setPrefHeight(85);
        note.setMaxHeight(100);
        VBox slotHistory = new VBox(10, AppUi.label("Slot history", "field-label"), historySlots);
        VBox notes = new VBox(10, AppUi.label("Select a completed consultation to load or save notes", "field-label"),
                historyBookings, note, AppUi.filters(load, save));
        slotHistory.setMinWidth(0);
        notes.setMinWidth(0);
        slotHistory.setPrefWidth(440);
        notes.setPrefWidth(440);
        HBox.setHgrow(slotHistory, Priority.ALWAYS);
        HBox.setHgrow(notes, Priority.ALWAYS);
        HBox columns = new HBox(24, slotHistory, notes);
        VBox content = new VBox(16, AppUi.label("Keep the conversation moving", "section-title"),
                AppUi.filters(AppUi.field("Date · SGT", historyDate), AppUi.field("Slot status", historyStatus), filter), columns);
        content.getStyleClass().add("content-section");
        VBox.setVgrow(columns, Priority.ALWAYS);
        VBox.setVgrow(historySlots, Priority.ALWAYS);
        VBox.setVgrow(historyBookings, Priority.ALWAYS);
        return new Tab("History & Notes", content);
    }

    private Instant instant(String value) {
        if (date.getValue() == null) { throw new IllegalArgumentException("Select a slot date"); }
        try {
            return LocalDateTime.of(date.getValue(), LocalTime.parse(value, TIME)).atZone(SINGAPORE).toInstant();
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Enter time as HH:mm");
        }
    }

    private static String displayTime(Instant value) {
        return DISPLAY_TIME.format(value.atZone(SINGAPORE));
    }

    private void refresh(String success) {
        try {
            modules.getItems().setAll(service.findActiveAssignedModules());
            modules.getItems().sort(Comparator.comparing(Module::code));
            bookingModules.getItems().setAll(modules.getItems());
            slots.getItems().setAll(date.getValue() == null
                    ? service.findUpcomingSlotViews()
                    : service.findUpcomingSlotViews(date.getValue()));
            bookings.getItems().setAll(service.findBookingViews(new BookingFilter(
                    bookingModules.getValue() == null ? null : bookingModules.getValue().id(),
                    bookingDate.getValue(), bookingStatus.getValue())));
            historySlots.getItems().setAll(service.findSlotHistory(historyDate.getValue(), historyStatus.getValue()));
            historyBookings.getItems().setAll(service.findBookingViews(new BookingFilter(null, null, BookingStatus.COMPLETED)));
            message.setText(success);
        } catch (RuntimeException failure) {
            modules.getItems().clear();
            slots.getItems().clear();
            bookingModules.getItems().clear();
            bookings.getItems().clear();
            historySlots.getItems().clear();
            historyBookings.getItems().clear();
            message.setText(failure instanceof IllegalArgumentException || failure instanceof SecurityException
                    ? failure.getMessage() : "Storage operation failed. Refresh and try again.");
        }
    }

    private void act(Runnable operation) {
        try { operation.run(); }
        catch (RuntimeException failure) {
            message.setText(failure instanceof IllegalArgumentException || failure instanceof SecurityException
                    ? failure.getMessage() : "Storage operation failed. Refresh and try again.");
            return;
        }
        refresh("Done");
    }

    private static <T> T selected(TableView<T> table) {
        T value = table.getSelectionModel().getSelectedItem();
        if (value == null) { throw new IllegalArgumentException("Select a record first"); }
        return value;
    }

    private static <T> TableView<T> table(String id) {
        TableView<T> table = new TableView<>();
        table.setId(id);
        table.setPlaceholder(AppUi.empty("No consultations to show", "Choose another date or refresh to see the latest records."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private static <T> void column(TableView<T> table, String name, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(name);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(150);
        table.getColumns().add(column);
    }

    private static TextField field(String prompt, String id) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setId(id);
        field.setPrefColumnCount(10);
        return field;
    }

    private static Button button(String text, String id, Runnable action) {
        Button button = new Button(text);
        button.setId(id);
        button.setOnAction(event -> action.run());
        return button;
    }

    private static <T> StringConverter<T> converter(Function<T, String> display) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : display.apply(value); }
            @Override public T fromString(String value) { throw new UnsupportedOperationException("Selection only"); }
        };
    }
}
