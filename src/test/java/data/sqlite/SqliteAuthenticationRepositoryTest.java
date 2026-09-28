package data.sqlite;

import java.nio.file.Path;
import java.util.UUID;
import model.user.Admin;
import model.user.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteAuthenticationRepositoryTest {
    private static final String HASH = "$argon2id$v=19$m=19456,t=2,p=1$c2FsdA$YWJjZGVmZ2hpamts";
    private static final String OTHER_HASH = "$argon2id$v=19$m=19456,t=2,p=1$bmV3c2FsdA$ZGVmZ2hpamtsbW5vcA";

    @TempDir Path directory;

    @Test
    void createAccount_userAndCredential_storesTogether() {
        Path database = directory.resolve("authentication.db");
        var authentication = SqliteRepositories.openAuthentication(database);
        Student student = new Student(UUID.randomUUID(), "Alice", "alice@example.edu", true);

        authentication.createAccount(student, HASH, false);

        assertEquals(student, SqliteRepositories.open(database).users().findById(student.id()).orElseThrow());
        var credential = SqliteRepositories.openAuthentication(database)
                .findCredentialByEmail("ALICE@example.edu").orElseThrow();
        assertEquals(student, credential.user());
        assertEquals(HASH, credential.encodedHash());
        assertFalse(credential.mustChangePassword());
    }

    @Test
    void updateCredential_existingAccount_persistsHashAndChangeFlag() {
        Path database = directory.resolve("update.db");
        var authentication = SqliteRepositories.openAuthentication(database);
        Student student = new Student(UUID.randomUUID(), "Alice", "alice@example.edu", true);
        authentication.createAccount(student, HASH, false);

        authentication.updateCredential(student.id(), OTHER_HASH, true);

        var credential = SqliteRepositories.openAuthentication(database)
                .findCredentialByEmail(student.email()).orElseThrow();
        assertEquals(OTHER_HASH, credential.encodedHash());
        assertTrue(credential.mustChangePassword());
    }

    @Test
    void bootstrapComplete_freshDatabase_returnsFalse() {
        var authentication = SqliteRepositories.openAuthentication(directory.resolve("bootstrap.db"));

        assertFalse(authentication.bootstrapComplete());
    }

    @Test
    void createInitialAdmin_freshDatabase_persistsAdminAndCompletesBootstrap() {
        Path database = directory.resolve("initial-admin.db");
        var authentication = SqliteRepositories.openAuthentication(database);
        Admin admin = new Admin(UUID.randomUUID(), "Root Admin", "admin@example.edu", true);

        assertEquals(admin, authentication.createInitialAdmin(admin, HASH));

        var reopened = SqliteRepositories.openAuthentication(database);
        var credential = reopened.findCredentialByEmail(admin.email()).orElseThrow();
        assertEquals(admin, credential.user());
        assertEquals(HASH, credential.encodedHash());
        assertFalse(credential.mustChangePassword());
        assertTrue(reopened.bootstrapComplete());
    }

    @Test
    void createInitialAdmin_duplicateEmail_leavesBootstrapIncomplete() {
        Path database = directory.resolve("duplicate-admin.db");
        var repositories = SqliteRepositories.open(database);
        Student legacyUser = new Student(UUID.randomUUID(), "Legacy", "legacy@example.edu", true);
        repositories.users().save(legacyUser);
        var authentication = SqliteRepositories.openAuthentication(database);
        Admin initialAdmin = new Admin(UUID.randomUUID(), "Root Admin", "LEGACY@example.edu", true);

        assertThrows(IllegalArgumentException.class,
                () -> authentication.createInitialAdmin(initialAdmin, HASH));

        assertFalse(authentication.bootstrapComplete());
        assertTrue(authentication.findCredentialByEmail(legacyUser.email()).isEmpty());
        assertTrue(repositories.users().findById(initialAdmin.id()).isEmpty());
    }

    @Test
    void createInitialAdmin_bootstrapAlreadyComplete_rejectsSecondAdmin() {
        var authentication = SqliteRepositories.openAuthentication(directory.resolve("second-admin.db"));
        Admin first = new Admin(UUID.randomUUID(), "First", "first@example.edu", true);
        Admin second = new Admin(UUID.randomUUID(), "Second", "second@example.edu", true);
        authentication.createInitialAdmin(first, HASH);

        assertThrows(IllegalArgumentException.class,
                () -> authentication.createInitialAdmin(second, OTHER_HASH));

        assertTrue(authentication.findCredentialByEmail(first.email()).isPresent());
        assertTrue(authentication.findCredentialByEmail(second.email()).isEmpty());
        assertTrue(authentication.bootstrapComplete());
    }

    @Test
    void createInitialAdmin_inactiveAdmin_rejectsWithoutCompletingBootstrap() {
        var authentication = SqliteRepositories.openAuthentication(directory.resolve("inactive-admin.db"));
        Admin inactive = new Admin(UUID.randomUUID(), "Inactive", "inactive@example.edu", false);

        assertThrows(IllegalArgumentException.class,
                () -> authentication.createInitialAdmin(inactive, HASH));

        assertFalse(authentication.bootstrapComplete());
        assertTrue(authentication.findCredentialByEmail(inactive.email()).isEmpty());
    }

    @Test
    void createAccount_adminBeforeBootstrap_rejectsSetupBypass() {
        Path database = directory.resolve("admin-bypass.db");
        var authentication = SqliteRepositories.openAuthentication(database);
        Admin admin = new Admin(UUID.randomUUID(), "Bypass", "bypass@example.edu", true);

        assertThrows(IllegalArgumentException.class,
                () -> authentication.createAccount(admin, HASH, false));

        assertFalse(authentication.bootstrapComplete());
        assertTrue(authentication.findCredentialByEmail(admin.email()).isEmpty());
    }

    @Test
    void createAccount_adminAfterBootstrap_allowsAdminProvisioning() {
        var authentication = SqliteRepositories.openAuthentication(directory.resolve("admin-provisioning.db"));
        Admin initial = new Admin(UUID.randomUUID(), "Initial", "initial@example.edu", true);
        Admin provisioned = new Admin(UUID.randomUUID(), "Additional", "additional@example.edu", true);
        authentication.createInitialAdmin(initial, HASH);

        authentication.createAccount(provisioned, OTHER_HASH, true);

        var credential = authentication.findCredentialByEmail(provisioned.email()).orElseThrow();
        assertEquals(provisioned, credential.user());
        assertEquals(OTHER_HASH, credential.encodedHash());
        assertTrue(credential.mustChangePassword());
    }

    @Test
    void createAccount_plaintextHash_rejectsPasswordStorage() {
        Path database = directory.resolve("plaintext.db");
        var authentication = SqliteRepositories.openAuthentication(database);
        Student student = new Student(UUID.randomUUID(), "Alice", "alice@example.edu", true);

        assertThrows(IllegalArgumentException.class,
                () -> authentication.createAccount(student, "plain-text-password", false));

        assertTrue(authentication.findCredentialByEmail(student.email()).isEmpty());
        assertTrue(SqliteRepositories.open(database).users().findById(student.id()).isEmpty());
    }
}
