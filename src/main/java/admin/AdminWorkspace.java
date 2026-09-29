package admin;

import authentication.AuthenticationService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import model.module.Module;
import model.user.Role;
import model.user.User;
import java.util.UUID;
import ui.AppUi;

/** JavaFX presentation; business checks live in AdminService. */
final class AdminWorkspace {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm")
            .withLocale(Locale.ENGLISH).withZone(ZoneId.of("Asia/Singapore"));
    private final AdminService service;
    private final AuthenticationService authentication;
    private final UUID adminId;
    private final Predicate<String> confirm;
    private final BorderPane root = new BorderPane();
    private final Label message = new Label();
    private final TableView<User> users = table("users-table");
    private final TableView<Module> modules = table("modules-table");
    private final TableView<AdminQueries.AssignmentRow> assignments = table("assignments-table");
    private final TableView<AdminQueries.BookingRow> bookings = table("bookings-table");
    private final TableView<AdminQueries.CountRow> moduleCounts = table("module-counts");
    private final TableView<AdminQueries.CountRow> tutorCounts = table("tutor-counts");
    private final Label statistics = new Label();
    private final ComboBox<User> tutors = new ComboBox<>();
    private final ComboBox<Module> moduleChoices = new ComboBox<>();

    AdminWorkspace(AdminService service, AuthenticationService authentication, UUID adminId,
                   Runnable signOut, Predicate<String> confirm) {
        this.service = service;
        this.authentication = authentication;
        this.adminId = adminId;
        this.confirm = confirm;
        Label heading = new Label("Admin workspace");
        heading.setId("role-heading");
        Button refresh = button("Refresh", "admin-refresh", () -> refresh("Refreshed"));
        Button logout = button("Sign out", "sign-out", signOut);
        TabPane tabs = new TabPane(usersTab(), modulesTab(), assignmentsTab(), bookingsTab(), statisticsTab());
        tabs.setId("admin-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        message.setId("admin-message");
        AppUi.workspace(root, heading, "Keep your consultation community running smoothly.", refresh, logout, tabs, message);
        refresh("");
    }

    Parent root() { return root; }

    private Tab usersTab() {
        column(users, "Name", User::name);
        column(users, "Email", User::email);
        column(users, "Role", u -> u.role().toString());
        column(users, "Status", u -> u.isActive() ? "Active" : "Inactive");
        TextField name = field("Name", "user-name");
        TextField email = field("Email", "user-email");
        ComboBox<Role> role = new ComboBox<>();
        role.setId("user-role");
        role.getItems().setAll(Role.TUTOR, Role.ADMIN);
        role.setValue(Role.TUTOR);
        PasswordField temporaryPassword = passwordField("temporary-password");
        PasswordField passwordConfirmation = passwordField("confirm-temporary-password");
        Button add = button("Create account", "add-user", () -> {
            try {
                act(() -> {
                    requireMatchingPasswords(temporaryPassword, passwordConfirmation);
                    authentication.provisionAccount(adminId, role.getValue(), name.getText(), email.getText(),
                            temporaryPassword.getText().toCharArray());
                    name.clear();
                    email.clear();
                });
            } finally {
                temporaryPassword.clear();
                passwordConfirmation.clear();
            }
        });
        Button reset = button("Reset selected password", "reset-password", () -> {
            try {
                act(() -> {
                    User selected = selected(users);
                    requireMatchingPasswords(temporaryPassword, passwordConfirmation);
                    authentication.resetPassword(adminId, selected.id(), temporaryPassword.getText().toCharArray());
                });
            } finally {
                temporaryPassword.clear();
                passwordConfirmation.clear();
            }
        });
        Button deactivate = button("Deactivate selected", "deactivate-user", () -> act(() -> {
            User selected = selected(users);
            if (confirm.test("Deactivate " + selected.name() + "? Existing history will be retained.")) {
                service.deactivateUser(selected.id());
            }
        }));
        Button reactivate = button("Reactivate selected", "reactivate-user", () -> act(() -> {
            User selected = selected(users);
            if (confirm.test("Reactivate " + selected.name() + "? This account will be able to sign in again.")) {
                service.reactivateUser(selected.id());
            }
        }));
        AppUi.primary(add);
        AppUi.primary(reset);
        AppUi.danger(deactivate);
        return new Tab("Users", AppUi.section("People make it possible",
                "Create Tutor or Admin accounts, reset passwords, or change account access.",
                AppUi.filters(AppUi.field("Full name", name), AppUi.field("Email address", email),
                        AppUi.field("Role", role), AppUi.field("Temporary password", temporaryPassword),
                        AppUi.field("Confirm temporary password", passwordConfirmation)),
                users, add, reset, deactivate, reactivate));
    }

    private Tab modulesTab() {
        column(modules, "Code", Module::code);
        column(modules, "Name", Module::name);
        column(modules, "Status", m -> m.isActive() ? "Active" : "Inactive");
        TextField code = field("Module code", "module-code");
        TextField name = field("Module name", "module-name");
        modules.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, value) -> {
            if (value != null) { code.setText(value.code()); name.setText(value.name()); }
        });
        Button add = button("Create", "add-module", () -> act(() -> {
            service.createModule(code.getText(), name.getText());
            code.clear(); name.clear();
        }));
        Button edit = button("Save selected", "edit-module", () -> act(() ->
                service.editModule(selected(modules).id(), code.getText(), name.getText())));
        Button deactivate = button("Deactivate selected", "deactivate-module", () -> act(() -> {
            Module chosen = selected(modules);
            if (confirm.test("Deactivate " + chosen.code() + "? Existing history will be retained.")) {
                service.deactivateModule(chosen.id());
            }
        }));
        AppUi.primary(add);
        AppUi.danger(deactivate);
        return new Tab("Modules", AppUi.section("The course catalogue", "Select a course to edit its details. Deactivation preserves its history.",
                AppUi.filters(AppUi.field("Course code", code), AppUi.field("Course name", name)), modules, add, edit, deactivate));
    }

    private Tab assignmentsTab() {
        column(assignments, "Tutor", AdminQueries.AssignmentRow::tutor);
        column(assignments, "Module", AdminQueries.AssignmentRow::module);
        tutors.setId("assignment-tutor");
        tutors.setPromptText("Active tutor");
        tutors.setConverter(converter(u -> u.name() + " (" + u.email() + ")"));
        moduleChoices.setId("assignment-module");
        moduleChoices.setPromptText("Active module");
        moduleChoices.setConverter(converter(Module::code));
        Button assign = button("Assign", "assign-tutor", () -> act(() -> {
            if (tutors.getValue() == null || moduleChoices.getValue() == null) {
                throw new IllegalArgumentException("Select an active tutor and module");
            }
            service.assign(tutors.getValue().id(), moduleChoices.getValue().id());
        }));
        Button unassign = button("Unassign selected", "unassign-tutor", () -> act(() -> {
            var chosen = selected(assignments);
            if (confirm.test("Unassign " + chosen.tutor() + " from " + chosen.module() + "?")) {
                service.unassign(chosen.tutorId(), chosen.moduleId());
            }
        }));
        AppUi.primary(assign);
        AppUi.danger(unassign);
        return new Tab("Assignments", AppUi.section("Connect tutors to courses", "Assign an active tutor so they can offer consultation slots.",
                AppUi.filters(AppUi.field("Tutor", tutors), AppUi.field("Course", moduleChoices)), assignments, assign, unassign));
    }

    private Tab bookingsTab() {
        column(bookings, "Student", AdminQueries.BookingRow::student);
        column(bookings, "Tutor", AdminQueries.BookingRow::tutor);
        column(bookings, "Module", AdminQueries.BookingRow::module);
        column(bookings, "Start (SGT)", b -> b.start() == null ? "Unavailable" : TIME.format(b.start()));
        column(bookings, "End (SGT)", b -> b.end() == null ? "Unavailable" : TIME.format(b.end()));
        column(bookings, "Status", b -> b.status().toString());
        return new Tab("Bookings", AppUi.section("Every consultation, in one place", "All bookings, including cancelled and completed consultations. Times shown in SGT.",
                new Region(), bookings));
    }

    private Tab statisticsTab() {
        statistics.setId("statistics-summary");
        statistics.setWrapText(true);
        moduleCounts.setPrefHeight(150);
        tutorCounts.setPrefHeight(150);
        column(moduleCounts, "Module", AdminQueries.CountRow::label);
        column(moduleCounts, "Bookings", c -> Long.toString(c.count()));
        column(tutorCounts, "Tutor", AdminQueries.CountRow::label);
        column(tutorCounts, "Bookings", c -> Long.toString(c.count()));
        statistics.getStyleClass().add("section-title");
        moduleCounts.setMinHeight(80);
        tutorCounts.setMinHeight(80);
        moduleCounts.setPlaceholder(new Label("No bookings yet"));
        tutorCounts.setPlaceholder(new Label("No bookings yet"));
        VBox byCourse = new VBox(12, AppUi.label("Bookings by course", "field-label"), moduleCounts);
        VBox byTutor = new VBox(12, AppUi.label("Bookings by tutor", "field-label"), tutorCounts);
        byCourse.setMinWidth(0);
        byTutor.setMinWidth(0);
        byCourse.setPrefWidth(440);
        byTutor.setPrefWidth(440);
        HBox.setHgrow(byCourse, Priority.ALWAYS);
        HBox.setHgrow(byTutor, Priority.ALWAYS);
        HBox breakdowns = new HBox(24, byCourse, byTutor);
        VBox layout = new VBox(18, AppUi.label("A view of your community", "section-title"), statistics,
                AppUi.label("All time · Rates include every booking status.", "muted"), breakdowns);
        VBox.setVgrow(breakdowns, Priority.ALWAYS);
        VBox.setVgrow(moduleCounts, Priority.ALWAYS);
        VBox.setVgrow(tutorCounts, Priority.ALWAYS);
        layout.getStyleClass().add("content-section");
        return new Tab("Statistics", layout);
    }

    private void refresh(String success) {
        try {
            AdminSnapshot data = service.load();
            users.getItems().setAll(data.users().stream()
                    .sorted(Comparator.comparing(User::name).thenComparing(User::id)).toList());
            modules.getItems().setAll(data.modules().stream().sorted(Comparator.comparing(Module::code)).toList());
            tutors.getItems().setAll(data.users().stream().filter(u -> u.role() == Role.TUTOR && u.isActive())
                    .sorted(Comparator.comparing(User::name)).toList());
            moduleChoices.getItems().setAll(data.modules().stream().filter(Module::isActive)
                    .sorted(Comparator.comparing(Module::code)).toList());
            tutors.setValue(null);
            moduleChoices.setValue(null);
            assignments.getItems().setAll(AdminQueries.assignments(data));
            bookings.getItems().setAll(AdminQueries.bookings(data));
            var totals = AdminQueries.statistics(data);
            statistics.setText(String.format(Locale.ENGLISH,
                    "Total bookings: %d    Completed: %d (%.1f%%)    Cancelled: %d (%.1f%%)",
                    totals.total(), totals.completed(), totals.completionRate(),
                    totals.cancelled(), totals.cancellationRate()));
            moduleCounts.getItems().setAll(totals.byModule());
            tutorCounts.getItems().setAll(totals.byTutor());
            message.setText(success);
        } catch (RuntimeException failure) {
            clearData();
            showFailure(failure, success.isEmpty() || success.equals("Refreshed") ? "" :
                    "The change succeeded, but refresh failed. ");
        }
    }

    private void act(Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException failure) {
            if (failure instanceof SecurityException) { clearData(); }
            showFailure(failure, "");
            return;
        }
        refresh("Done");
    }

    private void showFailure(RuntimeException failure, String prefix) {
        String detail = failure instanceof IllegalArgumentException || failure instanceof SecurityException
                ? failure.getMessage() : "Storage operation failed. Refresh and try again; see application diagnostics.";
        message.setText(prefix + detail);
    }

    private void clearData() {
        users.getItems().clear(); modules.getItems().clear(); assignments.getItems().clear();
        bookings.getItems().clear(); moduleCounts.getItems().clear(); tutorCounts.getItems().clear();
        tutors.getItems().clear(); tutors.setValue(null);
        moduleChoices.getItems().clear(); moduleChoices.setValue(null);
        statistics.setText("");
    }

    private static <T> T selected(TableView<T> table) {
        T value = table.getSelectionModel().getSelectedItem();
        if (value == null) { throw new IllegalArgumentException("Select a record first"); }
        return value;
    }

    private static <T> TableView<T> table(String id) {
        TableView<T> table = new TableView<>();
        table.setId(id);
        table.setPlaceholder(AppUi.empty("Nothing here yet", "Records will appear here as your community grows."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private static <T> void column(TableView<T> table, String name, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(name);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                setText(empty ? null : text);
                setTooltip(empty || text == null ? null : new Tooltip(text));
            }
        });
        column.setPrefWidth(160);
        table.getColumns().add(column);
    }

    private static TextField field(String prompt, String id) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setAccessibleText(prompt);
        field.setId(id);
        field.setPrefColumnCount(12);
        return field;
    }

    private static PasswordField passwordField(String id) {
        PasswordField field = new PasswordField();
        field.setId(id);
        field.setPrefColumnCount(12);
        return field;
    }

    private static void requireMatchingPasswords(PasswordField password, PasswordField confirmation) {
        if (!password.getText().equals(confirmation.getText())) {
            throw new IllegalArgumentException("Temporary passwords do not match");
        }
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
            @Override public T fromString(String value) { throw new UnsupportedOperationException("Select an item"); }
        };
    }
}
