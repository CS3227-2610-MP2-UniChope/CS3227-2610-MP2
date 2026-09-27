package shell;

import admin.AdminMainView;
import authentication.AuthenticatedUser;
import authentication.AuthenticationException;
import authentication.AuthenticationService;
import data.repository.Repositories;
import data.sqlite.SqliteRepositories;
import java.nio.file.Path;
import java.util.Map;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.user.Role;
import model.user.User;
import student.StudentMainView;
import tutor.TutorMainView;
import ui.AppUi;
import ui.Constellation;
import util.OperationLog;

public final class UniChopeApplication extends Application {
    private final Repositories repositories;
    private final Session session;
    private final AuthenticationService authentication;
    private final RoleRouter router = new RoleRouter(Map.of(
            Role.STUDENT, new StudentMainView(),
            Role.TUTOR, new TutorMainView(),
            Role.ADMIN, new AdminMainView()));
    private Stage stage;

    public UniChopeApplication() { this(SqliteRepositories.open(defaultDatabasePath())); }

    UniChopeApplication(Repositories repositories) {
        this.repositories = repositories;
        this.session = new Session(repositories.users());
        this.authentication = new AuthenticationService(repositories, OperationLog.application());
    }

    static Path defaultDatabasePath() {
        return Path.of("data", "unichope.db");
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        DemoData.seed(repositories);
        stage.setTitle("UniChope | Consultation booking");
        stage.setMinWidth(900);
        stage.setMinHeight(660);
        if (authentication.initialAdminSetupRequired()) { showInitialAdminSetup(); }
        else { showLogin(); }
        stage.show();
    }

    private void showInitialAdminSetup() {
        session.signOut();
        TextField name = textField("setup-name");
        TextField email = textField("setup-email");
        PasswordField password = passwordField("setup-password");
        PasswordField confirmation = passwordField("setup-confirm");
        Label error = feedback("setup-error");
        Button create = primaryButton("Create administrator", "setup-admin");
        create.setOnAction(event -> {
            try {
                authentication.setupInitialAdmin(name.getText(), email.getText(),
                        password.getText().toCharArray(), confirmation.getText().toCharArray());
                showLogin("Administrator created. Sign in to continue.");
            } catch (IllegalArgumentException failure) {
                error.setText(failure.getMessage());
            } catch (RuntimeException failure) {
                error.setText("Unable to create the administrator. Try again.");
            } finally {
                password.clear();
                confirmation.clear();
            }
        });
        VBox fields = new VBox(14,
                AppUi.field("Name", name), AppUi.field("Email", email),
                AppUi.field("Password (min 8 characters)", password),
                AppUi.field("Confirm password", confirmation), create, error);
        setAuthenticationPage("FIRST-TIME SETUP", "Set up UniChope.",
                "Create the first administrator", "This one-time account manages Tutor and Admin access.", fields);
    }

    private void showLogin() { showLogin(""); }

    private void showLogin(String notice) {
        session.signOut();
        TextField email = textField("login-email");
        PasswordField password = passwordField("login-password");
        Label error = feedback("login-error");
        error.setText(notice);
        Button login = primaryButton("Sign in", "sign-in");
        login.setDefaultButton(true);
        login.setOnAction(event -> {
            try {
                AuthenticatedUser user = authentication.login(email.getText(), password.getText().toCharArray());
                if (user.mustChangePassword()) { showPasswordChange(user); }
                else { openWorkspace(user); }
            } catch (AuthenticationException failure) {
                error.setText(failure.getMessage());
            } catch (RuntimeException failure) {
                error.setText("Unable to sign in right now. Try again.");
            } finally { password.clear(); }
        });
        Button register = new Button("Create student account");
        register.setId("student-signup");
        register.setMaxWidth(Double.MAX_VALUE);
        register.setOnAction(event -> showStudentRegistration());
        VBox fields = new VBox(14, AppUi.field("Email", email), AppUi.field("Password", password),
                login, register, error);
        setAuthenticationPage("CONSULTATIONS, CONNECTED", "Book time with a tutor\nin minutes.",
                "Welcome back", "Sign in with your UniChope account.", fields);
    }

    private void showStudentRegistration() {
        TextField name = textField("signup-name");
        TextField email = textField("signup-email");
        PasswordField password = passwordField("signup-password");
        PasswordField confirmation = passwordField("signup-confirm");
        Label error = feedback("signup-error");
        Button register = primaryButton("Create student account", "register-student");
        register.setOnAction(event -> {
            try {
                AuthenticatedUser student = authentication.registerStudent(name.getText(), email.getText(),
                        password.getText().toCharArray(), confirmation.getText().toCharArray());
                openWorkspace(student);
            } catch (IllegalArgumentException failure) {
                error.setText(failure.getMessage());
            } catch (RuntimeException failure) {
                error.setText("Unable to create the account. Check the details and try again.");
            } finally {
                password.clear();
                confirmation.clear();
            }
        });
        Button back = new Button("Back to sign in");
        back.setId("back-to-login");
        back.setMaxWidth(Double.MAX_VALUE);
        back.setOnAction(event -> showLogin());
        VBox fields = new VBox(14, AppUi.field("Name", name), AppUi.field("Email", email),
                AppUi.field("Password (min 8 characters)", password),
                AppUi.field("Confirm password", confirmation), register, back, error);
        setAuthenticationPage("STUDENT REGISTRATION", "Start with a good question.",
                "Create a student account", "Students can register here. Tutors and Admins are provisioned by an Admin.",
                fields);
    }

    private void showPasswordChange(AuthenticatedUser pending) {
        session.signOut();
        PasswordField current = passwordField("current-password");
        PasswordField password = passwordField("new-password");
        PasswordField confirmation = passwordField("confirm-new-password");
        Label error = feedback("change-password-error");
        Button change = primaryButton("Save password", "change-password");
        change.setDefaultButton(true);
        change.setOnAction(event -> {
            try {
                AuthenticatedUser changed = authentication.changePassword(pending.user().id(),
                        current.getText().toCharArray(), password.getText().toCharArray(),
                        confirmation.getText().toCharArray());
                openWorkspace(changed);
            } catch (AuthenticationException failure) {
                error.setText(failure.getMessage());
            } catch (IllegalArgumentException failure) {
                error.setText(failure.getMessage());
            } catch (RuntimeException failure) {
                error.setText("Unable to change the password right now. Try again.");
            } finally {
                current.clear();
                password.clear();
                confirmation.clear();
            }
        });
        Button signOut = new Button("Sign out");
        signOut.setId("sign-out");
        signOut.setMaxWidth(Double.MAX_VALUE);
        signOut.setOnAction(event -> showLogin());
        VBox fields = new VBox(14, AppUi.field("Current password", current),
                AppUi.field("New password (min 8 characters)", password),
                AppUi.field("Confirm new password", confirmation), change, signOut, error);
        setAuthenticationPage("PASSWORD UPDATE", "One quick security step.",
                "Choose a new password", "Change the temporary password before opening your workspace.", fields);
    }

    private void openWorkspace(AuthenticatedUser authenticated) {
        User user = session.signIn(authenticated);
        stage.getScene().setRoot(router.route(user).create(user, repositories, this::showLogin));
    }

    private void setAuthenticationPage(String eyebrow, String title, String formTitle,
                                       String subtitle, VBox fields) {
        Label titleLabel = AppUi.label(title, "login-title");
        Label description = AppUi.label("Connect with a tutor, ask your questions,\nand leave with a clearer direction.",
                "login-description");
        VBox hero = new VBox(20, AppUi.label(eyebrow, "login-eyebrow"), titleLabel, description,
                new Constellation());
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setMinWidth(0);
        HBox.setHgrow(hero, Priority.ALWAYS);

        VBox form = new VBox(20, AppUi.label(formTitle, "login-form-title"),
                AppUi.label(subtitle, "muted"), fields);
        form.getStyleClass().add("login-form");
        form.setPrefWidth(380);
        form.setMinWidth(340);
        form.setMaxWidth(400);
        form.setAlignment(Pos.CENTER_LEFT);
        HBox center = new HBox(36, hero, form);
        center.setAlignment(Pos.CENTER);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox navigation = new HBox(AppUi.brand(), spacer,
                AppUi.label("CONSULTATIONS, CONNECTED", "login-footer"));
        navigation.setAlignment(Pos.CENTER_LEFT);
        BorderPane content = new BorderPane(center, navigation, null,
                AppUi.label("Find a slot. Take conversation to the next step.", "login-footer"), null);
        content.setPadding(new Insets(32, 44, 28, 44));
        AppUi.theme(content);
        if (stage.getScene() == null) { stage.setScene(new Scene(content, 1180, 780)); }
        else { stage.getScene().setRoot(content); }
    }

    private static TextField textField(String id) {
        TextField field = new TextField();
        field.setId(id);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static PasswordField passwordField(String id) {
        PasswordField field = new PasswordField();
        field.setId(id);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static Label feedback(String id) {
        Label label = new Label();
        label.setId(id);
        label.getStyleClass().add("feedback");
        label.setWrapText(true);
        return label;
    }

    private static Button primaryButton(String text, String id) {
        Button button = new Button(text);
        button.setId(id);
        button.setMaxWidth(Double.MAX_VALUE);
        AppUi.primary(button);
        return button;
    }
}
