package shell;

import data.repository.Repositories;
import javafx.scene.Parent;
import model.user.User;

/** Role teams replace their placeholder implementation while retaining this boundary. */
@FunctionalInterface
public interface RoleView {
    Parent create(User user, Repositories repositories, Runnable signOut);
}
