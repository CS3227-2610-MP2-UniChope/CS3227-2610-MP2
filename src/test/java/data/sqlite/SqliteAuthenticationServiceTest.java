package data.sqlite;

import authentication.AuthenticationService;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.OperationLog;

import static org.junit.jupiter.api.Assertions.*;

class SqliteAuthenticationServiceTest {
    @TempDir Path directory;

    @Test
    void registerStudent_sqliteReopen_preservesCredentialAndStoredRole() {
        Path database = directory.resolve("service-auth.db");
        var clock = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"), ZoneOffset.UTC);
        var firstRepositories = SqliteRepositories.open(database);
        var firstService = new AuthenticationService(firstRepositories, new OperationLog(clock, event -> { }));
        firstService.setupInitialAdmin("Root Admin", "root@example.edu", "admin-password-123".toCharArray(),
                "admin-password-123".toCharArray());
        var student = firstService.registerStudent("Alice", "alice@example.edu", "student-password-123".toCharArray(),
                "student-password-123".toCharArray());

        var reopenedRepositories = SqliteRepositories.open(database);
        var reopenedService = new AuthenticationService(reopenedRepositories,
                new OperationLog(clock, event -> { }));
        var authenticated = reopenedService.login("ALICE@example.edu", "student-password-123".toCharArray());

        assertEquals(student.user(), authenticated.user());
        assertFalse(authenticated.mustChangePassword());
        assertFalse(reopenedService.initialAdminSetupRequired());
        assertEquals(student.user(), reopenedRepositories.users().findById(student.user().id()).orElseThrow());
    }
}
