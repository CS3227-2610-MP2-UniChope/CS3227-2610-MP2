package tutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.consultation.ConsultationSlot;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("ui")
class TutorAccessUiTest {
    @Test
    void refresh_inactiveTutor_clearsWorkspaceAndShowsMessage() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS));
        FutureTask<Void> scenario = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                var f = new TutorFixture();
                f.service.createSlot(f.module.id(), f.now.plusSeconds(3600), f.now.plusSeconds(5400));
                Parent root = new TutorWorkspace(f.service, () -> { }).root();
                stage.setScene(new Scene(root, 950, 620)); stage.show();
                f.data.users().save(f.tutor.withActive(false));
                ((Button) root.lookup("#tutor-refresh")).fire();
                @SuppressWarnings("unchecked") TableView<ConsultationSlot> slots =
                        (TableView<ConsultationSlot>) root.lookup("#slot-table");
                assertEquals(0, slots.getItems().size());
                assertTrue(((Label) root.lookup("#tutor-message")).getText().contains("active tutor"));
            } finally { stage.close(); }
            return null;
        });
        try { Platform.runLater(scenario); scenario.get(30, TimeUnit.SECONDS); }
        finally { Platform.exit(); }
    }
}
