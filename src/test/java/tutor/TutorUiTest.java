package tutor;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.module.Module;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("ui")
class TutorUiTest {
    @Test
    void slotsTab_activeTutorCreatesSlotWithModuleCode() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertEquals(true, started.await(15, TimeUnit.SECONDS));
        FutureTask<Void> scenario = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                var f = new TutorFixture();
                Parent root = new TutorWorkspace(f.service, () -> { }).root();
                stage.setScene(new Scene(root, 950, 620));
                stage.show();
                TabPane tabs = (TabPane) root.lookup("#tutor-tabs");
                assertNotNull(tabs);
                @SuppressWarnings("unchecked")
                ComboBox<Module> modules = (ComboBox<Module>) root.lookup("#slot-module");
                ((DatePicker) root.lookup("#slot-date")).setValue(LocalDate.of(2026, 9, 24));
                modules.setValue(f.module);
                ((TextField) root.lookup("#slot-start")).setText("10:00");
                ((TextField) root.lookup("#slot-end")).setText("11:00");
                ((Button) root.lookup("#create-slot")).fire();
                @SuppressWarnings("unchecked")
                TableView<TutorSlotView> slots = (TableView<TutorSlotView>) root.lookup("#slot-table");
                assertEquals(1, slots.getItems().size());
                assertEquals("CS3227", slots.getItems().getFirst().moduleCode());
                @SuppressWarnings("unchecked")
                TableColumn<TutorSlotView, String> startColumn =
                        (TableColumn<TutorSlotView, String>) slots.getColumns().get(1);
                assertEquals("24/09/2026 10:00", startColumn.getCellObservableValue(0).getValue());
                ((DatePicker) root.lookup("#slot-date")).setValue(LocalDate.of(2026, 9, 25));
                assertEquals(0, slots.getItems().size());
                ((DatePicker) root.lookup("#slot-date")).setValue(LocalDate.of(2026, 9, 24));
                assertEquals(1, slots.getItems().size());
                slots.getSelectionModel().selectFirst();
                ((Button) root.lookup("#cancel-slot")).fire();
                assertEquals(0, slots.getItems().size());
            } finally {
                stage.close();
            }
            return null;
        });
        try {
            Platform.runLater(scenario);
            scenario.get(30, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }
}
