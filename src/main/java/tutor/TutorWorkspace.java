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
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import model.consultation.ConsultationSlot;
import model.module.Module;

/** JavaFX presentation for tutor slot management; TutorService owns business rules. */
final class TutorWorkspace {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private final TutorService service;
    private final BorderPane root = new BorderPane();
    private final Label message = new Label();
    private final DatePicker date = new DatePicker(LocalDate.now(SINGAPORE));
    private final ComboBox<Module> modules = new ComboBox<>();
    private final TextField start = field("Start (HH:mm)", "slot-start");
    private final TextField end = field("End (HH:mm)", "slot-end");
    private final TableView<ConsultationSlot> slots = table("slot-table");

    TutorWorkspace(TutorService service, Runnable signOut) {
        this.service = service;
        Label heading = new Label("Tutor workspace");
        heading.setId("role-heading");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Button refresh = button("Refresh", "tutor-refresh", () -> refresh("Refreshed"));
        Button logout = button("Sign out", "sign-out", signOut);
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        root.setTop(new HBox(12, heading, space, refresh, logout));
        TabPane tabs = new TabPane(slotsTab());
        tabs.setId("tutor-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setCenter(tabs);
        message.setId("tutor-message");
        message.setWrapText(true);
        root.setBottom(message);
        root.setPadding(new Insets(18));
        BorderPane.setMargin(tabs, new Insets(16, 0, 12, 0));
        refresh("");
    }

    Parent root() { return root; }

    private Tab slotsTab() {
        column(slots, "Module", slot -> slot.moduleId().toString());
        column(slots, "Start (SGT)", slot -> slot.startTime().atZone(SINGAPORE).toLocalDateTime().toString());
        column(slots, "End (SGT)", slot -> slot.endTime().atZone(SINGAPORE).toLocalDateTime().toString());
        column(slots, "Status", slot -> slot.status().toString());
        date.setId("slot-date");
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
                service.cancelSlot(selected(slots).id())));
        return tab("Slots", slots, new FlowPane(8, 8, date, modules, start, end, create, cancel));
    }

    private Instant instant(String value) {
        if (date.getValue() == null) { throw new IllegalArgumentException("Select a slot date"); }
        try {
            return LocalDateTime.of(date.getValue(), LocalTime.parse(value, TIME)).atZone(SINGAPORE).toInstant();
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Enter time as HH:mm");
        }
    }

    private void refresh(String success) {
        try {
            modules.getItems().setAll(service.findActiveAssignedModules());
            modules.getItems().sort(Comparator.comparing(Module::code));
            slots.getItems().setAll(service.findUpcomingSlots(date.getValue()));
            message.setText(success);
        } catch (RuntimeException failure) {
            modules.getItems().clear();
            slots.getItems().clear();
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
        table.setPlaceholder(new Label("No records"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private static <T> void column(TableView<T> table, String name, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(name);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(150);
        table.getColumns().add(column);
    }

    private static Tab tab(String title, TableView<?> table, javafx.scene.Node controls) {
        VBox content = new VBox(10, controls, table);
        content.setPadding(new Insets(10, 0, 0, 0));
        VBox.setVgrow(table, Priority.ALWAYS);
        return new Tab(title, content);
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
