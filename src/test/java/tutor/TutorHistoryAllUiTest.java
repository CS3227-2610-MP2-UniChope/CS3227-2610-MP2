package tutor;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.user.Student;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("ui")
class TutorHistoryAllUiTest {
    @TempDir Path directory;

    @Test
    void showAllHistory_cancelledStatus_showsCancelledSlotsAcrossAllDates() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertEquals(true, started.await(15, TimeUnit.SECONDS));
        FutureTask<Void> scenario = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                var f = new TutorFixture(directory.resolve("tutor-history-all-ui.db"));
                ConsultationSlot completed = f.service.createSlot(f.module.id(),
                        f.now.plusSeconds(3600), f.now.plusSeconds(5400));
                ConsultationSlot cancelled = f.service.createSlot(f.module.id(),
                        f.now.plusSeconds(7200), f.now.plusSeconds(9000));
                f.service.cancelSlot(cancelled.id());
                Student student = new Student(UUID.randomUUID(), "Lin", "lin@example.edu", true);
                f.data.users().save(student);
                f.data.slots().save(completed.withStatus(SlotStatus.BOOKED));
                Booking booking = new Booking(UUID.randomUUID(), student.id(), completed.id(), f.now, BookingStatus.ACTIVE);
                f.data.bookings().save(booking);
                f.service.completeBooking(booking.id());

                Parent root = new TutorWorkspace(f.service, () -> { }).root();
                stage.setScene(new Scene(root, 950, 620));
                stage.show();
                @SuppressWarnings("unchecked")
                ComboBox<SlotStatus> status = (ComboBox<SlotStatus>) root.lookup("#history-status");
                status.setValue(SlotStatus.CANCELLED);
                ((Button) root.lookup("#show-all-history")).fire();

                assertNull(((DatePicker) root.lookup("#history-date")).getValue());
                assertEquals(SlotStatus.CANCELLED, status.getValue());
                @SuppressWarnings("unchecked")
                TableView<ConsultationSlot> slots = (TableView<ConsultationSlot>) root.lookup("#history-slot-table");
                assertEquals(List.of(cancelled.withStatus(SlotStatus.CANCELLED)), slots.getItems());
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
