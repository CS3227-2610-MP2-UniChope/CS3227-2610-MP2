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
    void registerStudent_sevenCharacterPassword_rejectsInput() {
        var service = new AuthenticationService(repositories(), new OperationLog(clock, event -> { }));

        assertThrows(IllegalArgumentException.class, () -> service.registerStudent("Alice", "alice@example.edu",
                "pass123".toCharArray(), "pass123".toCharArray()));
    }

    @Test
    void registerStudent_128CharacterPassword_acceptsAccount() {
        var service = new AuthenticationService(repositories(), new OperationLog(clock, event -> { }));
        String password = "p".repeat(128);

        AuthenticatedUser registered = service.registerStudent("Alice", "alice@example.edu",
                password.toCharArray(), password.toCharArray());

        assertEquals(Role.STUDENT,
                service.login(registered.user().email(), password.toCharArray()).user().role());
    }

    @Test
    void registerStudent_129CharacterPassword_rejectsInput() {
        var service = new AuthenticationService(repositories(), new OperationLog(clock, event -> { }));
        String password = "p".repeat(129);

        assertThrows(IllegalArgumentException.class, () -> service.registerStudent("Alice", "alice@example.edu",
                password.toCharArray(), password.toCharArray()));
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
    @Test
    void provisionStudentPersistsCredentialsRequiresPasswordChangeAndRejectsDuplicates() {
        Path database = directory.resolve("provision-student.db");
        var data = SqliteRepositories.open(database);
        var service = new AuthenticationService(data, new OperationLog(clock, event -> { }));
        var admin = service.setupInitialAdmin("Admin", "admin@example.edu",
                "admin-password-123".toCharArray(), "admin-password-123".toCharArray());
        char[] password = "temporary-password-123".toCharArray();
        var student = service.provisionAccount(admin.id(), Role.STUDENT, "New Student",
                "new-student@example.edu", password);
        assertArrayEquals(new char[password.length], password);

        var reopened = SqliteRepositories.open(database);
        var loginService = new AuthenticationService(reopened, new OperationLog(clock, event -> { }));
        var login = loginService.login(student.email(), "temporary-password-123".toCharArray());
        assertEquals(student, login.user());
        assertEquals(Role.STUDENT, login.user().role());
        assertTrue(login.mustChangePassword());
        assertFalse(loginService.changePassword(student.id(), "temporary-password-123".toCharArray(),
                "new-password-456".toCharArray(), "new-password-456".toCharArray()).mustChangePassword());
        assertThrows(AuthenticationException.class,
                () -> loginService.login(student.email(), "temporary-password-123".toCharArray()));

        data.users().save(student.withActive(false));
        char[] duplicatePassword = "duplicate-password-123".toCharArray();
        assertThrows(IllegalArgumentException.class, () -> service.provisionAccount(admin.id(),
                Role.STUDENT, "Duplicate", "NEW-STUDENT@example.edu", duplicatePassword));
        assertArrayEquals(new char[duplicatePassword.length], duplicatePassword);
        assertEquals(2, data.users().findAll().size());
        assertFalse(data.users().findById(student.id()).orElseThrow().isActive());
    }

    @Test
    void studentProvisioningRejectsUnauthorizedActorsAndInvalidInputsWithoutPartialAccounts() {
        var data = repositories();
        var service = new AuthenticationService(data, new OperationLog(clock, event -> { }));
        var admin = service.setupInitialAdmin("Admin", "admin@example.edu",
                "admin-password-123".toCharArray(), "admin-password-123".toCharArray());
        var student = service.registerStudent("Student", "student@example.edu",
                "student-password-123".toCharArray(), "student-password-123".toCharArray()).user();
        var tutor = service.provisionAccount(admin.id(), Role.TUTOR, "Tutor", "tutor@example.edu",
                "temporary-password-123".toCharArray());
        var pendingAdmin = service.provisionAccount(admin.id(), Role.ADMIN, "Pending", "pending@example.edu",
                "temporary-password-123".toCharArray());
        for (var actor : java.util.List.of(student.id(), tutor.id(), pendingAdmin.id(), UUID.randomUUID())) {
            char[] password = "temporary-password-123".toCharArray();
            assertThrows(SecurityException.class, () -> service.provisionAccount(actor, Role.STUDENT,
                    "Forbidden", "forbidden@example.edu", password));
            assertArrayEquals(new char[password.length], password);
        }
        assertThrows(IllegalArgumentException.class, () -> service.provisionAccount(admin.id(), null,
                "Invalid", "invalid@example.edu", "temporary-password-123".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> service.provisionAccount(admin.id(), Role.STUDENT,
                "Invalid", "invalid@example.edu", "short".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> service.provisionAccount(admin.id(), Role.STUDENT,
                " ", "invalid@example.edu", "temporary-password-123".toCharArray()));
        data.users().save(admin.withActive(false));
        assertThrows(SecurityException.class, () -> service.provisionAccount(admin.id(), Role.STUDENT,
                "Forbidden", "forbidden@example.edu", "temporary-password-123".toCharArray()));
        assertEquals(4, data.users().findAll().size());
        assertTrue(data.authentication().findCredentialByEmail("invalid@example.edu").isEmpty());
        assertTrue(data.authentication().findCredentialByEmail("forbidden@example.edu").isEmpty());
    }
}
