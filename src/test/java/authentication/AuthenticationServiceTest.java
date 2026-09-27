package authentication;

import data.sqlite.SqliteRepositories;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;
import model.user.Role;
import model.user.Admin;
import model.user.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.OperationLog;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {
    @TempDir Path directory;
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"), ZoneOffset.UTC);

    private data.repository.Repositories repositories() {
        return SqliteRepositories.open(directory.resolve(UUID.randomUUID() + ".db"));
    }

    @Test
    void registerStudent_validCredentials_createsAccountAndCanLogIn() {
        var repositories = repositories();
        var service = new AuthenticationService(repositories, new OperationLog(clock, event -> { }));
        char[] password = "student-password-123".toCharArray();
        char[] confirmation = "student-password-123".toCharArray();

        AuthenticatedUser registered = service.registerStudent(" Alice ", "ALICE@example.edu", password, confirmation);
        Student student = (Student) registered.user();
        AuthenticatedUser authenticated = service.login(student.email(), "student-password-123".toCharArray());

        assertEquals(student, registered.user());
        assertFalse(registered.mustChangePassword());
        assertEquals(Role.STUDENT, authenticated.user().role());
        assertFalse(authenticated.mustChangePassword());
        assertTrue(repositories.authentication().findCredentialByEmail(student.email()).isPresent());
        assertArrayEquals(new char[password.length], password);
        assertArrayEquals(new char[confirmation.length], confirmation);
    }

    @Test
    void registerStudent_eightCharacterPassword_acceptsAndCanLogIn() {
        var service = new AuthenticationService(repositories(), new OperationLog(clock, event -> { }));

        AuthenticatedUser registered = service.registerStudent("Alice", "alice@example.edu",
                "pass1234".toCharArray(), "pass1234".toCharArray());

        assertEquals(Role.STUDENT, service.login(registered.user().email(), "pass1234".toCharArray()).user().role());
    }

    @Test
    void login_unknownWrongOrInactiveAccount_usesSamePublicError() {
        var repositories = repositories();
        var service = new AuthenticationService(repositories, new OperationLog(clock, event -> { }));
        Student student = (Student) service.registerStudent("Alice", "alice@example.edu",
                "student-password-123".toCharArray(), "student-password-123".toCharArray()).user();
        repositories.users().save(student.withActive(false));

        String unknown = assertThrows(AuthenticationException.class,
                () -> service.login("missing@example.edu", "student-password-123".toCharArray())).getMessage();
        String wrong = assertThrows(AuthenticationException.class,
                () -> service.login(student.email(), "wrong-password-123".toCharArray())).getMessage();
        String inactive = assertThrows(AuthenticationException.class,
                () -> service.login(student.email(), "student-password-123".toCharArray())).getMessage();

        assertEquals(unknown, wrong);
        assertEquals(wrong, inactive);
    }

    @Test
    void setupInitialAdmin_beforeBootstrap_createsTheOnlyInitialAdmin() {
        var repositories = repositories();
        var service = new AuthenticationService(repositories, new OperationLog(clock, event -> { }));

        var admin = service.setupInitialAdmin("Root Admin", "root@example.edu",
                "admin-password-123".toCharArray(), "admin-password-123".toCharArray());

        assertEquals(Role.ADMIN, admin.role());
        assertTrue(repositories.authentication().bootstrapComplete());
        assertThrows(IllegalArgumentException.class, () -> service.setupInitialAdmin("Other", "other@example.edu",
                "admin-password-456".toCharArray(), "admin-password-456".toCharArray()));
    }

    @Test
    void changePassword_validCurrentPassword_replacesCredentialAndClearsChangeFlag() {
        var repositories = repositories();
        var service = new AuthenticationService(repositories, new OperationLog(clock, event -> { }));
        Student student = (Student) service.registerStudent("Alice", "alice@example.edu",
                "student-password-123".toCharArray(), "student-password-123".toCharArray()).user();

        AuthenticatedUser changed = service.changePassword(student.id(), "student-password-123".toCharArray(),
                "new-student-password-456".toCharArray(), "new-student-password-456".toCharArray());

        assertFalse(changed.mustChangePassword());
        assertThrows(AuthenticationException.class,
                () -> service.login(student.email(), "student-password-123".toCharArray()));
        assertFalse(service.login(student.email(), "new-student-password-456".toCharArray()).mustChangePassword());
    }

    @Test
    void provisionTutor_adminActor_setsTemporaryPasswordChangeRequirement() {
        var repositories = repositories();
        var service = new AuthenticationService(repositories, new OperationLog(clock, event -> { }));
        var admin = service.setupInitialAdmin("Root Admin", "root@example.edu",
                "admin-password-123".toCharArray(), "admin-password-123".toCharArray());

        var tutor = service.provisionAccount(admin.id(), Role.TUTOR, "Tutor", "tutor@example.edu",
                "temporary-password-789".toCharArray());
        AuthenticatedUser authenticated = service.login(tutor.email(), "temporary-password-789".toCharArray());

        assertEquals(Role.TUTOR, authenticated.user().role());
        assertTrue(authenticated.mustChangePassword());
    }

    @Test
    void provisionAccount_adminWithoutCredential_rejectsProvisioning() {
        var repositories = repositories();
        Admin legacyAdmin = new Admin(UUID.randomUUID(), "Legacy Admin", "legacy-admin@example.edu", true);
        repositories.users().save(legacyAdmin);
        var service = new AuthenticationService(repositories, new OperationLog(clock, event -> { }));

        assertThrows(SecurityException.class, () -> service.provisionAccount(legacyAdmin.id(), Role.TUTOR,
                "Tutor", "tutor@example.edu", "temporary-password-456".toCharArray()));

        assertTrue(repositories.users().findByEmail("tutor@example.edu").isEmpty());
    }

    @Test
    void login_failure_logging_doesNotIncludeCredentials() {
        var repositories = repositories();
        var events = new ArrayList<OperationLog.Event>();
        var service = new AuthenticationService(repositories, new OperationLog(clock, events::add));
        service.registerStudent("Alice", "alice@example.edu", "student-password-123".toCharArray(),
                "student-password-123".toCharArray());

        assertThrows(AuthenticationException.class,
                () -> service.login("alice@example.edu", "wrong-password-123".toCharArray()));

        assertEquals("auth.login", events.getLast().operation());
        assertEquals(OperationLog.Outcome.FAILURE, events.getLast().outcome());
        assertNull(events.getLast().entityId());
    }

    @Test
    void resetPassword_legacyUser_requiresChangeAtNextLogin() {
        var repositories = repositories();
        var service = new AuthenticationService(repositories, new OperationLog(clock, event -> { }));
        var admin = service.setupInitialAdmin("Root Admin", "root@example.edu",
                "admin-password-123".toCharArray(), "admin-password-123".toCharArray());
        Student legacy = new Student(UUID.randomUUID(), "Legacy", "legacy@example.edu", true);
        repositories.users().save(legacy);

        service.resetPassword(admin.id(), legacy.id(), "temporary-password-789".toCharArray());

        AuthenticatedUser login = service.login(legacy.email(), "temporary-password-789".toCharArray());
        assertTrue(login.mustChangePassword());
    }

    @Test
    void login_success_logsUserIdWithoutLoggingCredentials() {
        var repositories = repositories();
        var events = new ArrayList<OperationLog.Event>();
        var service = new AuthenticationService(repositories, new OperationLog(clock, events::add));
        Student student = (Student) service.registerStudent("Alice", "alice@example.edu",
                "student-password-123".toCharArray(), "student-password-123".toCharArray()).user();
        events.clear();

        service.login(student.email(), "student-password-123".toCharArray());

        assertEquals(1, events.size());
        assertEquals("auth.login", events.getFirst().operation());
        assertEquals(student.id(), events.getFirst().entityId());
        assertEquals(OperationLog.Outcome.SUCCESS, events.getFirst().outcome());
        assertEquals("", events.getFirst().errorType());
    }

    @Test
    void registerStudent_passwordMismatch_rejectsAndClearsBothInputs() {
        var service = new AuthenticationService(repositories(), new OperationLog(clock, event -> { }));
        char[] password = "student-password-123".toCharArray();
        char[] confirmation = "different-password-456".toCharArray();

        assertThrows(IllegalArgumentException.class,
                () -> service.registerStudent("Alice", "alice@example.edu", password, confirmation));

        assertArrayEquals(new char[password.length], password);
        assertArrayEquals(new char[confirmation.length], confirmation);
    }
}
