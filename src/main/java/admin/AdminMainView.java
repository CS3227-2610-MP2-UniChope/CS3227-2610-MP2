package admin;

import data.repository.Repositories;
import java.time.Clock;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import model.user.User;
import shell.RoleView;
import util.OperationLog;

/** Admin-owned entry point; existing shell/other role contracts stay unchanged. */
public final class AdminMainView implements RoleView {
    @Override
    public Parent create(User user, Repositories repositories, Runnable signOut) {
        AdminService service = new AdminService(repositories, user.id(), Clock.systemUTC(),
                OperationLog.application());
        return new AdminWorkspace(service, signOut, message -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle("Confirm change");
            alert.setHeaderText("Preserve consultation history");
            return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
        }).root();
    }
}
