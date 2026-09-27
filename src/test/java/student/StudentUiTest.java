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
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
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

    @Test void studentChoosesDateFiltersTimelineBooksAndCancels() throws Exception {
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
                UUID otherTutor = UUID.randomUUID();
                UUID otherModule = UUID.randomUUID();
                Instant now = Instant.parse("2026-09-26T02:00:00Z");
                Clock clock = Clock.fixed(now, ZoneOffset.UTC);
                data.users().save(new Student(studentId, "Student", "student@example.edu", true));
                data.users().save(new Tutor(tutorId, "Dr. Mira Tan", "mira@example.edu", true));
                data.users().save(new Tutor(otherTutor, "Prof. Arun Lee", "arun@example.edu", true));
                data.modules().save(new Module(moduleId, "CS1101S", "Programming Methodology", true));
                data.modules().save(new Module(otherModule, "CS2040S", "Data Structures and Algorithms", true));
                data.modules().assign(new TutorModule(tutorId, moduleId));
                data.modules().assign(new TutorModule(otherTutor, otherModule));
                var first = data.slots().save(new ConsultationSlot(UUID.randomUUID(), tutorId, moduleId,
                        now.plusSeconds(3600), now.plusSeconds(5400), SlotStatus.AVAILABLE));
                var later = data.slots().save(new ConsultationSlot(UUID.randomUUID(), tutorId, moduleId,
                        now.plusSeconds(14400), now.plusSeconds(18000), SlotStatus.AVAILABLE));
                var other = data.slots().save(new ConsultationSlot(UUID.randomUUID(), otherTutor, otherModule,
                        now.plusSeconds(3600), now.plusSeconds(7200), SlotStatus.AVAILABLE));
                var midnight = data.slots().save(new ConsultationSlot(UUID.randomUUID(), otherTutor, otherModule,
                        now.plusSeconds(14 * 3600), now.plusSeconds(14 * 3600 + 1800), SlotStatus.AVAILABLE));
                var service = new StudentService(data, studentId, clock, new OperationLog(clock, event -> { }));
                AtomicBoolean accept = new AtomicBoolean(false);
                Parent root = new StudentWorkspace(service, () -> { }, prompt -> accept.get()).root();
                stage.setScene(new Scene(root, 1180, 780));
                stage.show();
                assertNotNull(root.lookup("#student-calendar"));
                assertNull(root.lookup("#student-book"));
                assertTrue(button(root, "student-date-2026-09-25").isDisabled());
                assertTrue(button(root, "student-date-2026-09-26").getAccessibleText().contains("3 available slots"));
                assertTrue(button(root, "student-date-2026-09-27").getAccessibleText().contains("1 available slots"));
                snapshot(service, null, null, "student-calendar", 1180, 780);
                snapshot(service, null, null, "student-calendar-small", 850, 550);
                button(root, "student-calendar-next").fire();
                assertEquals("October 2026", label(root, "student-calendar-month").getText());
                button(root, "student-calendar-previous").fire();
                assertEquals("September 2026", label(root, "student-calendar-month").getText());
                button(root, "student-date-2026-09-28").fire();
                assertTrue(label(root, "student-day-count").getText().startsWith("0 available"));
                assertTrue(button(root, "student-book").isDisabled());
                button(root, "student-back-calendar").fire();
                button(root, "student-date-2026-09-26").fire();
                layout(root);
                assertNotNull(root.lookup("#student-slot-" + first.id()));
                assertNull(root.lookup("#student-slot-" + midnight.id()));
                assertTrue(button(root, "student-book").isDisabled());
                selectSlot(root, first.id());
                assertFalse(button(root, "student-book").isDisabled());
                assertTrue(label(root, "student-slot-selection").getText().contains("Mira"));
                snapshot(service, LocalDate.of(2026, 9, 26), first.id(), "student-slot-timetable", 1180, 780);
                snapshot(service, LocalDate.of(2026, 9, 26), first.id(), "student-slot-timetable-small", 850, 550);
                field(root, "student-slot-module").setText("missing");
                button(root, "student-slot-search").fire();
                layout(root);
                assertNull(root.lookup("#student-slot-" + first.id()));
                assertTrue(button(root, "student-book").isDisabled());
                field(root, "student-slot-module").setText(" PROGRAMMING ");
                field(root, "student-slot-tutor").setText("mira");
                button(root, "student-slot-search").fire();
                layout(root);
                assertNotNull(root.lookup("#student-slot-" + first.id()));
                assertNotNull(root.lookup("#student-slot-" + later.id()));
                assertNull(root.lookup("#student-slot-" + other.id()));
                button(root, "student-slot-clear").fire();
                layout(root);
                assertNotNull(root.lookup("#student-slot-" + other.id()));
                assertEquals("", field(root, "student-slot-module").getText());
                assertTrue(label(root, "student-selected-date").getText().contains("26 Sep"));
                selectSlot(root, first.id());
                button(root, "student-book").fire();
                layout(root);
                assertEquals(SlotStatus.BOOKED, data.slots().findById(first.id()).orElseThrow().status());
                assertNull(root.lookup("#student-slot-" + first.id()));
                assertTrue(button(root, "student-book").isDisabled());
                button(root, "student-back-calendar").fire();
                assertTrue(button(root, "student-date-2026-09-26").getAccessibleText().contains("2 available slots"));
                button(root, "student-date-2026-09-27").fire();
                layout(root);
                assertNotNull(root.lookup("#student-slot-" + midnight.id()));
                ((TabPane) root.lookup("#student-tabs")).getSelectionModel().select(1);
                layout(root);
                @SuppressWarnings("unchecked")
                TableView<StudentService.BookingRow> bookings = (TableView<StudentService.BookingRow>) root.lookup("#student-bookings");
                assertEquals(1, bookings.getItems().size());
                field(root, "student-booking-course").setText("missing");
                button(root, "student-booking-search").fire();
                assertTrue(bookings.getItems().isEmpty());
                field(root, "student-booking-course").setText(" cs1101s ");
                field(root, "student-booking-tutor").setText("MIRA");
                DatePicker bookingDate = (DatePicker) root.lookup("#student-booking-date");
                bookingDate.setValue(LocalDate.of(2026, 9, 26));
                button(root, "student-booking-search").fire();
                assertEquals(1, bookings.getItems().size());
                button(root, "student-booking-clear").fire();
                assertNull(bookingDate.getValue());
                bookings.getSelectionModel().selectFirst();
                button(root, "student-cancel").fire();
                assertEquals(SlotStatus.BOOKED, data.slots().findById(first.id()).orElseThrow().status());
                accept.set(true);
                button(root, "student-cancel").fire();
                assertEquals(SlotStatus.AVAILABLE, data.slots().findById(first.id()).orElseThrow().status());
                assertEquals("CANCELLED", bookings.getItems().getFirst().status().toString());
                assertEquals("Done", label(root, "student-message").getText());
                ((TabPane) root.lookup("#student-tabs")).getSelectionModel().select(0);
                button(root, "student-day-previous").fire();
                layout(root);
                assertNotNull(root.lookup("#student-slot-" + first.id()));
                selectSlot(root, later.id());
                data.slots().save(later.withStatus(SlotStatus.CANCELLED));
                button(root, "student-book").fire();
                layout(root);
                assertNull(root.lookup("#student-slot-" + later.id()));
                assertTrue(button(root, "student-book").isDisabled());
                assertNotEquals("Done", label(root, "student-message").getText());
            } finally { stage.close(); }
            return null;
        });
        try {
            Platform.runLater(scenario);
            scenario.get(45, TimeUnit.SECONDS);
        } finally { Platform.exit(); }
    }

    private static void layout(Parent root) { root.applyCss(); root.layout(); }
    // A fresh scene avoids cached-text artifacts when multiple snapshots run in one FX pulse.
    private static void snapshot(StudentService service, LocalDate date, UUID slot, String name,
                                 double width, double height) throws Exception {
        Parent preview = new StudentWorkspace(service, () -> { }, prompt -> false).root();
        new Scene(preview, width, height);
        preview.resize(width, height);
        layout(preview);
        if (date != null) {
            button(preview, "student-date-" + date).fire();
            selectSlot(preview, slot);
        }
        support.UiSnapshots.save(preview, name);
    }
    private static Button button(Parent root, String id) { layout(root); return (Button) root.lookup("#" + id); }
    private static Label label(Parent root, String id) { return (Label) root.lookup("#" + id); }
    private static TextField field(Parent root, String id) { return (TextField) root.lookup("#" + id); }
    private static void selectSlot(Parent root, UUID id) {
        layout(root);
        ((ToggleButton) root.lookup("#student-slot-" + id)).fire();
    }
}
