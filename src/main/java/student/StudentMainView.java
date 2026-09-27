package student;

import data.repository.Repositories;
import java.time.Clock;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import model.user.Role;
import model.user.User;
import shell.RoleView;
import util.OperationLog;

public final class StudentMainView implements RoleView {
    @Override
    public Parent create(User user, Repositories repositories, Runnable signOut) {
        if (user.role() != Role.STUDENT || !user.isActive()) {
            throw new IllegalArgumentException("An active student account is required");
        }
        StudentService service = new StudentService(repositories, user.id(), Clock.systemUTC(),
                OperationLog.application());
        return new StudentWorkspace(service, signOut, message -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle("Cancel booking");
            alert.setHeaderText("Release this consultation slot?");
            return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
        }).root();
    }
}
