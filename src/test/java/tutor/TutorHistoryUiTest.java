package tutor;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import model.consultation.Booking;
import model.consultation.BookingStatus;
import model.consultation.ConsultationSlot;
import model.consultation.SlotStatus;
import model.user.Student;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("ui")
class TutorHistoryUiTest {
    @Test
    void historyTab_completedBooking_savesAndLoadsNote() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertEquals(true, started.await(15, TimeUnit.SECONDS));
        FutureTask<Void> scenario = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                var f = new TutorFixture();
                Student student = new Student(UUID.randomUUID(), "Lin", "lin@example.edu", true);
                f.data.users().save(student);
                ConsultationSlot slot = f.service.createSlot(f.module.id(), f.now.plusSeconds(3600), f.now.plusSeconds(5400));
                f.data.slots().save(slot.withStatus(SlotStatus.BOOKED));
                Booking booking = new Booking(UUID.randomUUID(), student.id(), slot.id(), f.now, BookingStatus.ACTIVE);
                f.data.bookings().save(booking);
                f.service.completeBooking(booking.id());
                Parent root = new TutorWorkspace(f.service, () -> { }).root();
                stage.setScene(new Scene(root, 950, 620)); stage.show();
                @SuppressWarnings("unchecked") TableView<TutorBookingView> bookings =
                        (TableView<TutorBookingView>) root.lookup("#history-booking-table");
                bookings.getSelectionModel().selectFirst();
                TextArea note = (TextArea) root.lookup("#note-content");
                note.setText("Discussed testing");
                ((Button) root.lookup("#save-note")).fire();
                note.clear();
                ((Button) root.lookup("#load-note")).fire();
                assertEquals("Discussed testing", note.getText());
            } finally { stage.close(); }
            return null;
        });
        try { Platform.runLater(scenario); scenario.get(30, TimeUnit.SECONDS); }
        finally { Platform.exit(); }
    }
}
