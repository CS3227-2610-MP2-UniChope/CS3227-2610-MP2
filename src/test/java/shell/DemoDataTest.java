package shell;

import data.sqlite.SqliteRepositories;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import model.module.Module;
import model.user.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DemoDataTest {
    @TempDir Path directory;

    @Test
    void seedsNusCsCurriculumModulesAsActiveAndOnlyOnce() {
        var data = SqliteRepositories.open(directory.resolve("fresh.db"));

        DemoData.seed(data);
        var modules = data.modules().findAll();
        assertEquals(15, modules.size());
        assertTrue(modules.stream().allMatch(Module::isActive));
        assertEquals(Set.of("CS1101S", "ES2660", "IS1108", "CS1231S", "CS2030S", "CS2040S",
                "CS2100", "CS2101", "CS2103T", "CS2106", "CS2109S", "CS3230", "MA1521",
                "MA1522", "ST2334"), modules.stream().map(Module::code).collect(Collectors.toSet()));
        assertEquals("Digital and AI Ethics", modules.stream().filter(m -> m.code().equals("IS1108"))
                .findFirst().orElseThrow().name());

        DemoData.seed(data);
        assertEquals(modules, data.modules().findAll());
        assertEquals(3, data.users().findAll().size());
    }

    @Test
    void backfillsModulesWithoutChangingExistingUsersOrModuleEdits() {
        var data = SqliteRepositories.open(directory.resolve("existing.db"));
        Student student = new Student(UUID.randomUUID(), "Existing", "existing@example.edu", true);
        Module custom = new Module(UUID.randomUUID(), "cs1101s", "Custom name", false);
        data.users().save(student);
        data.modules().save(custom);

        DemoData.seed(data);
        assertEquals(Set.of(student), Set.copyOf(data.users().findAll()));
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
}
