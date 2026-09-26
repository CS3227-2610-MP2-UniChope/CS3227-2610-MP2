package shell;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UniChopeApplicationTest {
    @Test
    void defaultDatabasePath_projectRoot_returnsDataDatabase() {
        assertEquals(Path.of("data", "unichope.db"), UniChopeApplication.defaultDatabasePath());
    }
}
