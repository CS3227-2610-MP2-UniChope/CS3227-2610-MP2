package tutor;

import data.repository.Repositories;
import java.time.Clock;
import javafx.scene.Parent;
import model.user.User;
import shell.RoleView;
import util.OperationLog;

/** Integration placeholder owned by the tutor role team. */
public final class TutorMainView implements RoleView {
    @Override
    public Parent create(User user, Repositories repositories, Runnable signOut) {
        TutorService service = new TutorService(repositories, user.id(), Clock.systemUTC(), OperationLog.application());
        return new TutorWorkspace(service, signOut).root();
    }
}
