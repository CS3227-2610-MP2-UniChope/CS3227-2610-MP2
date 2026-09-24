package shell;

import admin.AdminMainView;
import data.memory.InMemoryRepositories;
import data.repository.Repositories;
import java.util.Map;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import model.user.Role;
import model.user.User;
import student.StudentMainView;
import tutor.TutorMainView;

public final class UniChopeApplication extends Application {
    private final Repositories repositories = InMemoryRepositories.create();
    private final Session session = new Session(repositories.users());
    private final RoleRouter router = new RoleRouter(Map.of(
            Role.STUDENT, new StudentMainView(),
            Role.TUTOR, new TutorMainView(),
            Role.ADMIN, new AdminMainView()));
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        DemoData.seed(repositories);
        stage.setTitle("UniChope — Foundation Preview");
        stage.setMinWidth(620);
        stage.setMinHeight(380);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        session.signOut();
        Label title = new Label("UniChope");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        Label notice = new Label("Demo login — select a sample account. No password authentication. "
                + "Data is held in memory and resets when the app closes.");
        notice.setWrapText(true);
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
        Button login = new Button("Continue");
        login.setId("sign-in");
        login.setDefaultButton(true);
        login.disableProperty().bind(accounts.valueProperty().isNull());
        login.setOnAction(event -> {
            try {
                User user = session.signIn(accounts.getValue().id());
                stage.getScene().setRoot(router.route(user).create(user, repositories, this::showLogin));
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
            }
        });
        VBox content = new VBox(18, title, notice, accounts, login, error);
        content.setPadding(new Insets(32));
        if (stage.getScene() == null) {
            stage.setScene(new Scene(content, 760, 440));
        } else {
            stage.getScene().setRoot(content);
        }
    }
}
