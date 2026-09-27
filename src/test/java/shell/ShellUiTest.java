package shell;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real JavaFX scene smoke test. Run separately with uiTest on a desktop. */
@Tag("ui")
class ShellUiTest {
    @Test
    void loginAndLogoutWorkForAllThreeRoles() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS));
        FutureTask<Void> test = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                new UniChopeApplication().start(stage);
                snapshot(stage, "login");
                var loginRoot = stage.getScene().getRoot();
                loginRoot.resize(900, 660);
                support.UiSnapshots.save(loginRoot, "login-small");
                loginRoot.resize(1180, 780);
                for (String role : new String[] {"Student", "Tutor", "Admin"}) {
                    var root = stage.getScene().getRoot();
                    ComboBox<?> accounts = (ComboBox<?>) root.lookup("#account-selector");
                    Button login = (Button) root.lookup("#sign-in");
                    assertTrue(login.isDisabled());
                    int index = switch (role) { case "Student" -> 0; case "Tutor" -> 1; default -> 2; };
                    accounts.getSelectionModel().select(index);
                    login.fire();
                    Label heading = (Label) stage.getScene().getRoot().lookup("#role-heading");
                    assertEquals(role + " workspace", heading.getText());
                    snapshot(stage, role.toLowerCase());
                    ((Button) stage.getScene().getRoot().lookup("#sign-out")).fire();
                    assertNotNull(stage.getScene().getRoot().lookup("#account-selector"));
                }
            } finally {
                stage.close();
            }
            return null;
        });
        try {
            Platform.runLater(test);
            test.get(30, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }

    private static void snapshot(Stage stage, String name) throws Exception {
        support.UiSnapshots.save(stage.getScene().getRoot(), name);
    }
}
