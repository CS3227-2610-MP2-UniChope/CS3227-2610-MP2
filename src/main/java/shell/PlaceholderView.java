package shell;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.user.User;

/** Shared presentation only; future role features belong in their own packages. */
public final class PlaceholderView {
    private PlaceholderView() { }

    public static Parent create(String title, String description, User user, Runnable signOut) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        heading.setId("role-heading");
        Label identity = new Label("Signed in as " + user.name() + " (" + user.email() + ")");
        Label details = new Label(description);
        details.setWrapText(true);
        Button logout = new Button("Sign out");
        logout.setId("sign-out");
        logout.setOnAction(event -> signOut.run());
        VBox layout = new VBox(18, heading, identity, details,
                new Label("Foundation preview — role features are not implemented yet."), logout);
        layout.setPadding(new Insets(32));
        return layout;
    }
}
