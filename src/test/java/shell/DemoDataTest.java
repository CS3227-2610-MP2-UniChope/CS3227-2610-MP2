package shell;

import authentication.AuthenticationService;
import data.sqlite.SqliteRepositories;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import model.module.Module;
import model.user.Role;
import model.user.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.OperationLog;
import static org.junit.jupiter.api.Assertions.*;

class DemoDataTest {
    @TempDir Path directory;

    @Test
    void seedFreshDatabase_addsCurriculumAndDefaultAdmin() {
        var data = SqliteRepositories.open(directory.resolve("fresh.db"));

        DemoData.seed(data);
        var modules = data.modules().findAll();
        var admin = data.users().findByEmail(DemoData.DEFAULT_ADMIN_EMAIL).orElseThrow();
        assertEquals(1, data.users().findAll().size());
        assertEquals(Role.ADMIN, admin.role());
        assertTrue(admin.isActive());
        var authentication = new AuthenticationService(data,
                new OperationLog(Clock.systemUTC(), event -> { }));
        assertEquals(admin, authentication.login(DemoData.DEFAULT_ADMIN_EMAIL,
                DemoData.DEFAULT_ADMIN_PASSWORD.toCharArray()).user());
        assertFalse(authentication.initialAdminSetupRequired());
        assertEquals(15, modules.size());
        assertTrue(modules.stream().allMatch(Module::isActive));
        assertEquals(Set.of("CS1101S", "ES2660", "IS1108", "CS1231S", "CS2030S", "CS2040S",
                "CS2100", "CS2101", "CS2103T", "CS2106", "CS2109S", "CS3230", "MA1521",
                "MA1522", "ST2334"), modules.stream().map(Module::code).collect(Collectors.toSet()));
        assertEquals("Digital and AI Ethics", modules.stream().filter(m -> m.code().equals("IS1108"))
                .findFirst().orElseThrow().name());

        DemoData.seed(data);
        assertEquals(modules, data.modules().findAll());
        assertEquals(1, data.users().findAll().size());
    }

    @Test
    void backfillsModulesWithoutChangingExistingUsersOrModuleEdits() {
        var data = SqliteRepositories.open(directory.resolve("existing.db"));
        Student student = new Student(UUID.randomUUID(), "Existing", "existing@example.edu", true);
        Module custom = new Module(UUID.randomUUID(), "cs1101s", "Custom name", false);
        data.users().save(student);
        data.modules().save(custom);

        DemoData.seed(data);
        assertEquals(Set.of(student, data.users().findByEmail(DemoData.DEFAULT_ADMIN_EMAIL).orElseThrow()),
                Set.copyOf(data.users().findAll()));
        assertEquals(custom, data.modules().findById(custom.id()).orElseThrow());
        assertEquals(15, data.modules().findAll().size());

        Module original = data.modules().findAll().stream().filter(m -> m.code().equals("CS2030S"))
                .findFirst().orElseThrow();
        Module edited = data.modules().save(new Module(original.id(), "CS2030X", "Edited title", false));
        DemoData.seed(data);
        assertEquals(15, data.modules().findAll().size());
        assertEquals(edited, data.modules().findById(original.id()).orElseThrow());
        assertTrue(data.modules().findAll().stream().noneMatch(m -> m.code().equals("CS2030S")));
    }

    @Test
    void seedInitializedDatabase_preservesExistingAdminWithoutAddingDefaultAdmin() {
        var data = SqliteRepositories.open(directory.resolve("initialized.db"));
        var authentication = new AuthenticationService(data,
                new OperationLog(Clock.systemUTC(), event -> { }));
        var existing = authentication.setupInitialAdmin("Existing Admin", "existing-admin@u.nus.edu",
                "existing-password".toCharArray(), "existing-password".toCharArray());

        DemoData.seed(data);

        assertEquals(Set.of(existing), Set.copyOf(data.users().findAll()));
        assertTrue(data.users().findByEmail(DemoData.DEFAULT_ADMIN_EMAIL).isEmpty());
    }
}
