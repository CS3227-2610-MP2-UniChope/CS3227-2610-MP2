package shell;

import data.memory.InMemoryRepositories;
import java.util.Map;
import java.util.UUID;
import model.user.Role;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionRoutingTest {
    @Test
    void everyDemoUserRoutesToTheirOwnViewAndLogoutClearsSession() {
        var repositories = InMemoryRepositories.create();
        DemoData.seed(repositories);
        var session = new Session(repositories.users());
        RoleView student = (user, data, out) -> null;
        RoleView tutor = (user, data, out) -> null;
        RoleView admin = (user, data, out) -> null;
        var views = Map.of(Role.STUDENT, student, Role.TUTOR, tutor, Role.ADMIN, admin);
        var router = new RoleRouter(views);
        for (var user : repositories.users().findAll()) {
            assertEquals(user, session.signIn(user.id()));
            assertSame(views.get(user.role()), router.route(session.currentUser().orElseThrow()));
            session.signOut();
            assertTrue(session.currentUser().isEmpty());
        }
    }

    @Test
    void inactiveOrMissingUsersCannotSignInAndFailedLoginClearsSession() {
        var repositories = InMemoryRepositories.create();
        DemoData.seed(repositories);
        var user = repositories.users().findAll().getFirst();
        var session = new Session(repositories.users());
        session.signIn(user.id());
        repositories.users().save(user.withActive(false));
        assertTrue(session.currentUser().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> session.signIn(user.id()));
        assertThrows(IllegalArgumentException.class, () -> session.signIn(UUID.randomUUID()));
        assertTrue(session.currentUser().isEmpty());
        RoleView view = (u, data, out) -> null;
        var router = new RoleRouter(Map.of(Role.STUDENT, view, Role.TUTOR, view, Role.ADMIN, view));
        assertThrows(IllegalArgumentException.class, () -> router.route(user.withActive(false)));
        assertThrows(NullPointerException.class, () -> new RoleRouter(Map.of(Role.STUDENT, view)));
    }
}
