package admin;

import data.repository.Repositories;
import javafx.scene.Parent;
import model.user.Role;
import model.user.User;
import shell.PlaceholderView;
import shell.RoleView;

/** Integration placeholder owned by the admin role team. */
public final class AdminMainView implements RoleView {
    @Override
    public Parent create(User user, Repositories repositories, Runnable signOut) {
        if (user.role() != Role.ADMIN || !user.isActive()) {
            throw new IllegalArgumentException("An active admin account is required");
        }
        return PlaceholderView.create("Admin workspace", "Manage users, modules, tutor assignments, and system-wide booking statistics.", user, signOut);
    }
}
