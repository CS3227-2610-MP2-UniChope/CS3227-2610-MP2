package shell;

import authentication.AuthenticationService;
import data.repository.Repositories;
import data.sqlite.SqliteRepositories;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.OperationLog;

import static org.junit.jupiter.api.Assertions.*;

/** Real JavaFX authentication flow test. Run separately with uiTest on a desktop. */
@Tag("ui")
class ShellUiTest {
    @TempDir Path directory;

    @Test
    void authenticationScreens_defaultAdminLoginPasswordChangeAndStudentSignupWork() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS));
        Repositories repositories = SqliteRepositories.open(directory.resolve("shell.db"));
        var authentication = new AuthenticationService(repositories,
                new OperationLog(Clock.systemUTC(), event -> { }));
        FutureTask<Void> test = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                new UniChopeApplication(repositories).start(stage);
                assertNotNull(stage.getScene().getRoot().lookup("#login-email"));

                var admin = repositories.users().findByEmail(DemoData.DEFAULT_ADMIN_EMAIL).orElseThrow();
                authentication.provisionAccount(admin.id(), model.user.Role.TUTOR, "Tutor", "tutor@example.edu",
                        "temporary-password-456".toCharArray());
                text(stage, "#login-email", DemoData.DEFAULT_ADMIN_EMAIL);
                password(stage, "#login-password", "wrong-password-123");
                click(stage, "#sign-in");
                assertEquals("Email or password is incorrect.", ((Label) stage.getScene().getRoot()
                        .lookup("#login-error")).getText());
                password(stage, "#login-password", DemoData.DEFAULT_ADMIN_PASSWORD);
                click(stage, "#sign-in");
                assertEquals("Admin workspace", ((Label) stage.getScene().getRoot()
                        .lookup("#role-heading")).getText());

                click(stage, "#sign-out");
                text(stage, "#login-email", "tutor@example.edu");
                password(stage, "#login-password", "temporary-password-456");
                click(stage, "#sign-in");
                assertNotNull(stage.getScene().getRoot().lookup("#new-password"));
                password(stage, "#current-password", "temporary-password-456");
                password(stage, "#new-password", "tutor-new-password-789");
                password(stage, "#confirm-new-password", "tutor-new-password-789");
                click(stage, "#change-password");
                assertEquals("Tutor workspace", ((Label) stage.getScene().getRoot()
                        .lookup("#role-heading")).getText());

                click(stage, "#sign-out");
                click(stage, "#student-signup");
                assertNotNull(stage.getScene().getRoot().lookup("#signup-name"));
                text(stage, "#signup-name", "Alice");
                text(stage, "#signup-email", "alice@example.edu");
                password(stage, "#signup-password", "student-password-123");
                password(stage, "#signup-confirm", "student-password-123");
                click(stage, "#register-student");
                assertEquals("Student workspace", ((Label) stage.getScene().getRoot()
                        .lookup("#role-heading")).getText());
            } finally {
                stage.close();
            }
            return null;
        });
        try {
            Platform.runLater(test);
            test.get(60, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }

    private static void text(Stage stage, String selector, String value) {
        ((TextField) stage.getScene().getRoot().lookup(selector)).setText(value);
    }

    private static void password(Stage stage, String selector, String value) {
        ((PasswordField) stage.getScene().getRoot().lookup(selector)).setText(value);
    }

    private static void click(Stage stage, String selector) {
        ((Button) stage.getScene().getRoot().lookup(selector)).fire();
    }
}
