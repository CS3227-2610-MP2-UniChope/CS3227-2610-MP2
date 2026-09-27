package shell;

import admin.AdminMainView;
import data.repository.Repositories;
import data.sqlite.SqliteRepositories;
import java.nio.file.Path;
import java.util.Map;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import model.user.Role;
import model.user.User;
import student.StudentMainView;
import tutor.TutorMainView;
import ui.AppUi;
import ui.Constellation;

public final class UniChopeApplication extends Application {
    private final Repositories repositories = SqliteRepositories.open(defaultDatabasePath());
    private final Session session = new Session(repositories.users());
    private final RoleRouter router = new RoleRouter(Map.of(
            Role.STUDENT, new StudentMainView(),
            Role.TUTOR, new TutorMainView(),
            Role.ADMIN, new AdminMainView()));
    private Stage stage;

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
        showLogin();
        stage.show();
    }

    private void showLogin() {
        session.signOut();
        Label title = AppUi.label("Book time with a tutor \nin minutes.", "login-title");
        Label description = AppUi.label("Connect with a tutor, ask your questions,\nand leave with a clearer direction.", "login-description");
        Label notice = AppUi.label("This is a demo account selector. No password is required. Your data stays on this device.", "muted");
        ComboBox<User> accounts = new ComboBox<>();
        accounts.setId("account-selector");
        accounts.setMaxWidth(Double.MAX_VALUE);
        accounts.getItems().setAll(repositories.users().findAll().stream().filter(User::isActive).toList());
        accounts.setPromptText("Choose an account");
        accounts.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.name() + " — " + user.role();
            }
            @Override
            public User fromString(String text) { throw new UnsupportedOperationException("Selection only"); }
        });
        Label error = new Label();
        error.setId("login-error");
        error.getStyleClass().add("feedback");
        error.setWrapText(true);
        Button login = new Button("Continue");
        login.setId("sign-in");
        login.setDefaultButton(true);
        login.setMaxWidth(Double.MAX_VALUE);
        AppUi.primary(login);
        login.disableProperty().bind(accounts.valueProperty().isNull());
        login.setOnAction(event -> {
            try {
                User user = session.signIn(accounts.getValue().id());
                stage.getScene().setRoot(router.route(user).create(user, repositories, this::showLogin));
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
            }
        });
        VBox hero = new VBox(20, AppUi.label("A SPACE FOR BETTER QUESTIONS", "login-eyebrow"),
                title, description, new Constellation());
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setMinWidth(0);
        HBox.setHgrow(hero, Priority.ALWAYS);
        VBox accountField = AppUi.field("Demo account", accounts);
        accounts.setPrefWidth(310);
        VBox form = new VBox(20, AppUi.label("Welcome back", "login-form-title"),
                AppUi.label("Choose your account to open your workspace.", "muted"), accountField, login, notice, error);
        form.getStyleClass().add("login-form");
        form.setPrefWidth(350);
        form.setMinWidth(320);
        form.setMaxWidth(350);
        form.setAlignment(Pos.CENTER_LEFT);
        HBox center = new HBox(36, hero, form);
        center.setAlignment(Pos.CENTER);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox navigation = new HBox(AppUi.brand(), spacer, AppUi.label("CONSULTATIONS, CONNECTED", "login-footer"));
        navigation.setAlignment(Pos.CENTER_LEFT);
        BorderPane content = new BorderPane(center, navigation, null,
                AppUi.label("Find a slot. Take conversation to the next step.", "login-footer"), null);
        content.setPadding(new Insets(32, 44, 28, 44));
        AppUi.theme(content);
        if (stage.getScene() == null) {
            stage.setScene(new Scene(content, 1180, 780));
        } else {
            stage.getScene().setRoot(content);
        }
    }
}
