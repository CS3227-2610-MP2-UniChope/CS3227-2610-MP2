package student;

import data.repository.Repositories;
import javafx.scene.Parent;
import model.user.Role;
import model.user.User;
import shell.PlaceholderView;
import shell.RoleView;

/** Integration placeholder owned by the student role team. */
public final class StudentMainView implements RoleView {
    @Override
    public Parent create(User user, Repositories repositories, Runnable signOut) {
        if (user.role() != Role.STUDENT || !user.isActive()) {
            throw new IllegalArgumentException("An active student account is required");
        }
        return PlaceholderView.create("Student workspace", "Browse slots, book consultations, and view your consultation history.", user, signOut);
    }
}
