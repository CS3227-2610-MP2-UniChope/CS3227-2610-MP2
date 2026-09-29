package shell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UniversalLauncherTest {
    @Test
    void selectsNativeLibrariesForSupportedSystems() {
        assertEquals("win", UniversalLauncher.platform("Windows 11", "amd64"));
        assertEquals("linux", UniversalLauncher.platform("Linux", "amd64"));
        assertEquals("linux-aarch64", UniversalLauncher.platform("Linux", "aarch64"));
        assertEquals("mac", UniversalLauncher.platform("Mac OS X", "x86_64"));
        assertEquals("mac-aarch64", UniversalLauncher.platform("Mac OS X", "aarch64"));
        assertEquals("mac-aarch64", UniversalLauncher.platform("Darwin", "arm64"));
    }

    @Test
    void rejectsUnsupportedSystemsBeforeLoadingNativeCode() {
        assertThrows(IllegalArgumentException.class, () -> UniversalLauncher.platform("Windows 11", "aarch64"));
        assertThrows(IllegalArgumentException.class, () -> UniversalLauncher.platform("Linux", "x86"));
        assertThrows(IllegalArgumentException.class, () -> UniversalLauncher.platform("FreeBSD", "amd64"));
    }
}
