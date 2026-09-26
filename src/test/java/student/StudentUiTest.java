package student;

import data.memory.InMemoryRepositories;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.module.Module;
import model.module.TutorModule;
import model.user.Student;
import model.user.Tutor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import util.OperationLog;
import static org.junit.jupiter.api.Assertions.*;

@Tag("ui")
class StudentUiTest {
    @Test
    void studentBooksCancelsAndSeesReleasedSlot() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS));
        FutureTask<Void> scenario = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                var data = InMemoryRepositories.create();
                UUID studentId = UUID.randomUUID();
                UUID tutorId = UUID.randomUUID();
                UUID moduleId = UUID.randomUUID();
                Instant now = Instant.parse("2026-09-26T02:00:00Z");
                Clock clock = Clock.fixed(now, ZoneOffset.UTC);
                data.users().save(new Student(studentId, "Student", "student@example.edu", true));
                data.users().save(new Tutor(tutorId, "Tutor", "tutor@example.edu", true));
                data.modules().save(new Module(moduleId, "CS1", "Module", true));
                data.modules().assign(new TutorModule(tutorId, moduleId));
                var slot = data.slots().save(new ConsultationSlot(UUID.randomUUID(), tutorId, moduleId,
                        now.plusSeconds(3600), now.plusSeconds(5400), SlotStatus.AVAILABLE));
                var service = new StudentService(data, studentId, clock, new OperationLog(clock, event -> { }));
                AtomicBoolean accept = new AtomicBoolean(false);
                Parent root = new StudentWorkspace(service, () -> { }, prompt -> accept.get()).root();
                stage.setScene(new Scene(root, 850, 550));
                stage.show();
                @SuppressWarnings("unchecked")
                TableView<StudentService.SlotRow> slots = (TableView<StudentService.SlotRow>) root.lookup("#student-slots");
                @SuppressWarnings("unchecked")
                TableView<StudentService.BookingRow> bookings = (TableView<StudentService.BookingRow>) root.lookup("#student-bookings");
                assertEquals(1, slots.getItems().size());
                slots.getSelectionModel().selectFirst();
                ((Button) root.lookup("#student-book")).fire();
                assertEquals(SlotStatus.BOOKED, data.slots().findById(slot.id()).orElseThrow().status());
                assertTrue(slots.getItems().isEmpty());
                assertEquals(1, bookings.getItems().size());
                bookings.getSelectionModel().selectFirst();
                ((Button) root.lookup("#student-cancel")).fire();
                assertEquals(SlotStatus.BOOKED, data.slots().findById(slot.id()).orElseThrow().status());
                accept.set(true);
                ((Button) root.lookup("#student-cancel")).fire();
                assertEquals(SlotStatus.AVAILABLE, data.slots().findById(slot.id()).orElseThrow().status());
                assertEquals(1, slots.getItems().size());
                assertEquals("CANCELLED", bookings.getItems().getFirst().status().toString());
                assertEquals("Done", ((Label) root.lookup("#student-message")).getText());
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
