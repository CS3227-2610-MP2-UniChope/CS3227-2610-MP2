package shell;

import data.repository.Repositories;
import java.util.UUID;
import model.user.Admin;
import model.user.Student;
import model.user.Tutor;

/** Sample identities only. This is not an authentication mechanism. */
public final class DemoData {
    private DemoData() { }

    public static void seed(Repositories repositories) {
        if (!repositories.users().findAll().isEmpty()) { return; }
        repositories.users().save(new Student(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Demo Student", "student@example.edu", true));
        repositories.users().save(new Tutor(UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "Demo Tutor", "tutor@example.edu", true));
        repositories.users().save(new Admin(UUID.fromString("00000000-0000-0000-0000-000000000003"),
                "Demo Admin", "admin@example.edu", true));
    }
}
