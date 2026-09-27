package shell;

import authentication.AuthenticatedUser;
import authentication.AuthenticationService;
import data.sqlite.SqliteRepositories;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import model.user.Role;
import model.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.OperationLog;

import static org.junit.jupiter.api.Assertions.*;

class SessionRoutingTest {
    @TempDir Path directory;
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void signIn_authenticatedUsers_routesByStoredRolesAndClearsOnSignOut() {
        var repositories = SqliteRepositories.open(directory.resolve("routing.db"));
        var authentication = new AuthenticationService(repositories, new OperationLog(clock, event -> { }));
        var admin = authentication.setupInitialAdmin("Admin", "admin@example.edu",
                "admin-password-123".toCharArray(), "admin-password-123".toCharArray());
        var tutor = authentication.provisionAccount(admin.id(), Role.TUTOR, "Tutor", "tutor@example.edu",
                "temporary-password-456".toCharArray());
        authentication.changePassword(tutor.id(), "temporary-password-456".toCharArray(),
                "tutor-password-789".toCharArray(), "tutor-password-789".toCharArray());
        var student = authentication.registerStudent("Student", "student@example.edu",
                "student-password-789".toCharArray(), "student-password-789".toCharArray());
        Map<Role, RoleView> views = Map.of(
                Role.STUDENT, (user, data, out) -> null,
                Role.TUTOR, (user, data, out) -> null,
                Role.ADMIN, (user, data, out) -> null);
        var router = new RoleRouter(views);
        var session = new Session(repositories.users());

        for (var login : new Login[] {
                new Login(admin.email(), "admin-password-123", Role.ADMIN),
                new Login(tutor.email(), "tutor-password-789", Role.TUTOR),
                new Login(student.user().email(), "student-password-789", Role.STUDENT)}) {
            AuthenticatedUser verified = authentication.login(login.email(), login.password().toCharArray());
            assertEquals(login.role(), session.signIn(verified).role());
            assertSame(views.get(login.role()), router.route(session.currentUser().orElseThrow()));
            session.signOut();
            assertTrue(session.currentUser().isEmpty());
        }
    }

    @Test
    void signIn_inactiveOrPasswordChangeRequiredUser_rejectsWorkspaceAccess() {
        var repositories = SqliteRepositories.open(directory.resolve("inactive.db"));
        var authentication = new AuthenticationService(repositories, new OperationLog(clock, event -> { }));
        var admin = authentication.setupInitialAdmin("Admin", "admin@example.edu",
                "admin-password-123".toCharArray(), "admin-password-123".toCharArray());
        var pending = authentication.provisionAccount(admin.id(), Role.TUTOR, "Tutor", "tutor@example.edu",
                "temporary-password-456".toCharArray());
        AuthenticatedUser pendingLogin = authentication.login(pending.email(), "temporary-password-456".toCharArray());
        var session = new Session(repositories.users());

        assertTrue(pendingLogin.mustChangePassword());
        assertThrows(IllegalArgumentException.class, () -> session.signIn(pendingLogin));
        assertTrue(session.currentUser().isEmpty());

        repositories.users().save(pending.withActive(false));
        assertThrows(authentication.AuthenticationException.class,
                () -> authentication.login(pending.email(), "temporary-password-456".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> session.signIn(
                new AuthenticatedUser(pending.withActive(true), false)));
        assertTrue(session.currentUser().isEmpty());
    }

    private record Login(String email, String password, Role role) { }
}
