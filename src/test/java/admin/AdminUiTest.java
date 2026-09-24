package admin;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import model.consultation.*;
import model.module.Module;
import model.user.Role;
import model.user.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("ui")
class AdminUiTest {
    @Test
    void adminWorkflowValidationRefreshConfirmationAndRevocation() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS));
        FutureTask<Void> scenario = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                var f = new AdminFixture();
                var signedOut = new AtomicBoolean();
                var accepted = new AtomicBoolean(true);
                Parent root = new AdminWorkspace(f.service, () -> signedOut.set(true), text -> accepted.get()).root();
                stage.setScene(new Scene(root, 950, 620));
                stage.show();
                TabPane tabs = (TabPane) root.lookup("#admin-tabs");
                assertEquals(5, tabs.getTabs().size());
                select(root, tabs, 0);
                field(root, "user-name").setText("New student");
                field(root, "user-email").setText("new@example.edu");
                button(root, "add-user").fire();
                User added = f.data.users().findByEmail("new@example.edu").orElseThrow();
                field(root, "user-name").setText("Duplicate");
                field(root, "user-email").setText("NEW@example.edu");
                button(root, "add-user").fire();
                assertTrue(message(root).contains("Email already exists"));
                assertEquals(4, f.data.users().findAll().size());
                TableView<User> users = typedTable(root, "users-table");
                users.getSelectionModel().select(added);
                accepted.set(false);
                button(root, "deactivate-user").fire();
                assertTrue(f.data.users().findById(added.id()).orElseThrow().isActive());
                accepted.set(true);
                users.getSelectionModel().select(added);
                button(root, "deactivate-user").fire();
                assertFalse(f.data.users().findById(added.id()).orElseThrow().isActive());
                snapshot(stage, "users");

                select(root, tabs, 1);
                field(root, "module-code").setText("CS9999");
                field(root, "module-name").setText("New module");
                button(root, "add-module").fire();
                Module addedModule = f.data.modules().findAll().stream().filter(m -> m.code().equals("CS9999"))
                        .findFirst().orElseThrow();
                TableView<Module> modules = typedTable(root, "modules-table");
                modules.getSelectionModel().select(addedModule);
                field(root, "module-name").setText("Edited module");
                button(root, "edit-module").fire();
                assertEquals("Edited module", f.data.modules().findById(addedModule.id()).orElseThrow().name());
                snapshot(stage, "modules");

                select(root, tabs, 2);
                button(root, "assign-tutor").fire();
                assertTrue(message(root).contains("Select an active tutor"));
                ComboBox<User> tutors = typedChoice(root, "assignment-tutor");
                ComboBox<Module> choices = typedChoice(root, "assignment-module");
                tutors.setValue(f.tutor);
                choices.setValue(f.module);
                button(root, "assign-tutor").fire();
                assertEquals(1, f.data.modules().findAssignments().size());
                var slot = f.slot(f.clock.instant().plusSeconds(3600), SlotStatus.AVAILABLE);
                TableView<AdminQueries.AssignmentRow> assignments = typedTable(root, "assignments-table");
                assignments.getSelectionModel().selectFirst();
                button(root, "unassign-tutor").fire();
                assertTrue(message(root).contains("future available slots"));
                assertEquals(1, f.data.modules().findAssignments().size());
                f.data.slots().save(slot.withStatus(SlotStatus.CANCELLED));
                assignments.getSelectionModel().selectFirst();
                button(root, "unassign-tutor").fire();
                assertTrue(f.data.modules().findAssignments().isEmpty());
                snapshot(stage, "assignments");

                var completedSlot = f.slot(f.clock.instant().minusSeconds(3600), SlotStatus.COMPLETED);
                f.booking(completedSlot, BookingStatus.COMPLETED);
                f.booking(completedSlot, BookingStatus.CANCELLED);
                button(root, "admin-refresh").fire();
                select(root, tabs, 3);
                TableView<AdminQueries.BookingRow> bookings = typedTable(root, "bookings-table");
                assertEquals(2, bookings.getItems().size());
                assertTrue(bookings.getColumns().get(3).getCellData(0).toString().contains("07:00"));
                snapshot(stage, "bookings");
                select(root, tabs, 4);
                String summary = ((Label) root.lookup("#statistics-summary")).getText();
                assertTrue(summary.contains("Total bookings: 2"));
                assertTrue(summary.contains("Completed: 1 (50.0%)"));
                assertTrue(summary.contains("Cancelled: 1 (50.0%)"));
                snapshot(stage, "statistics");

                f.data.users().save(f.actor.withActive(false));
                button(root, "admin-refresh").fire();
                assertTrue(message(root).contains("active admin"));
                assertTrue(bookings.getItems().isEmpty());
                assertTrue(users.getItems().isEmpty());
                button(root, "sign-out").fire();
                assertTrue(signedOut.get());
            } finally { stage.close(); }
            return null;
        });
        try { Platform.runLater(scenario); scenario.get(45, TimeUnit.SECONDS); }
        finally { Platform.exit(); }
    }

    private static void select(Parent root, TabPane tabs, int index) {
        tabs.getSelectionModel().select(index);
        root.applyCss(); root.layout();
    }
    private static TextField field(Parent root, String id) { return (TextField) root.lookup("#" + id); }
    private static Button button(Parent root, String id) { return (Button) root.lookup("#" + id); }
    private static String message(Parent root) { return ((Label) root.lookup("#admin-message")).getText(); }
    @SuppressWarnings("unchecked")
    private static <T> TableView<T> typedTable(Parent root, String id) {
        return (TableView<T>) root.lookup("#" + id);
    }
    @SuppressWarnings("unchecked")
    private static <T> ComboBox<T> typedChoice(Parent root, String id) {
        return (ComboBox<T>) root.lookup("#" + id);
    }
    private static void snapshot(Stage stage, String name) throws Exception {
        var image = stage.getScene().snapshot(null);
        BufferedImage output = new BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < output.getHeight(); y++) {
            for (int x = 0; x < output.getWidth(); x++) {
                output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        Path directory = Path.of("build", "reports", "admin-snapshots");
        Files.createDirectories(directory);
        ImageIO.write(output, "png", directory.resolve(name + ".png").toFile());
    }
}
