package student;

import data.sqlite.SqliteRepositories;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.module.Module;
import model.module.TutorModule;
import model.user.Student;
import model.user.Tutor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.OperationLog;
import static org.junit.jupiter.api.Assertions.*;

@Tag("ui")
class StudentUiTest {
    @TempDir Path directory;
    @Test
    void studentBooksCancelsAndSeesReleasedSlot() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS));
        FutureTask<Void> scenario = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                var data = SqliteRepositories.open(directory.resolve("student-ui.db"));
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
                TextField moduleSearch = (TextField) root.lookup("#student-slot-module");
                TextField tutorSearch = (TextField) root.lookup("#student-slot-tutor");
                DatePicker slotDate = (DatePicker) root.lookup("#student-slot-date");
                moduleSearch.setText("missing");
                ((Button) root.lookup("#student-slot-search")).fire();
                assertTrue(slots.getItems().isEmpty());
                moduleSearch.setText(" cs1 ");
                tutorSearch.setText("tut");
                slotDate.setValue(LocalDate.of(2026, 9, 26));
                ((Button) root.lookup("#student-slot-search")).fire();
                assertEquals(1, slots.getItems().size());
                ((Button) root.lookup("#student-slot-clear")).fire();
                assertEquals(1, slots.getItems().size());
                assertEquals("", moduleSearch.getText());
                assertEquals("", tutorSearch.getText());
                assertNull(slotDate.getValue());
                slots.getSelectionModel().selectFirst();
                ((Button) root.lookup("#student-book")).fire();
                assertEquals(SlotStatus.BOOKED, data.slots().findById(slot.id()).orElseThrow().status());
                assertTrue(slots.getItems().isEmpty());
                assertEquals(1, bookings.getItems().size());
                TextField bookingCourse = (TextField) root.lookup("#student-booking-course");
                TextField bookingTutor = (TextField) root.lookup("#student-booking-tutor");
                DatePicker bookingDate = (DatePicker) root.lookup("#student-booking-date");
                bookingCourse.setText("missing");
                ((Button) root.lookup("#student-booking-search")).fire();
                assertTrue(bookings.getItems().isEmpty());
                bookingCourse.setText(" cs1 ");
                bookingTutor.setText("TUT");
                bookingDate.setValue(LocalDate.of(2026, 9, 26));
                ((Button) root.lookup("#student-booking-search")).fire();
                assertEquals(1, bookings.getItems().size());
                ((Button) root.lookup("#student-booking-clear")).fire();
                assertEquals(1, bookings.getItems().size());
                assertEquals("", bookingCourse.getText());
                assertEquals("", bookingTutor.getText());
                assertNull(bookingDate.getValue());
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
