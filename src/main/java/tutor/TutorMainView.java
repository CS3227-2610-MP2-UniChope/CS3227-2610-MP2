package tutor;

import data.repository.Repositories;
import javafx.scene.Parent;
import model.user.Role;
import model.user.User;
import shell.PlaceholderView;
import shell.RoleView;

/** Integration placeholder owned by the tutor role team. */
public final class TutorMainView implements RoleView {
    @Override
    public Parent create(User user, Repositories repositories, Runnable signOut) {
        if (user.role() != Role.TUTOR || !user.isActive()) {
            throw new IllegalArgumentException("An active tutor account is required");
        }
        return PlaceholderView.create("Tutor workspace", "Manage your consultation slots, bookings, and completed consultation notes.", user, signOut);
    }
}
