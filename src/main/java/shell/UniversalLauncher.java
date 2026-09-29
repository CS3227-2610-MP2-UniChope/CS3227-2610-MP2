package shell;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.jar.JarFile;

/** Loads intact dependency JARs for the current OS and CPU from the release JAR. */
public final class UniversalLauncher {
    private UniversalLauncher() { }

    static String platform(String osName, String architecture) {
        String os = osName.toLowerCase(Locale.ROOT);
        String arch = architecture.toLowerCase(Locale.ROOT);
        boolean arm = arch.equals("aarch64") || arch.equals("arm64");
        boolean x64 = arch.equals("amd64") || arch.equals("x86_64");
        if (os.startsWith("windows") && x64) { return "win"; }
        if (os.startsWith("linux") && (x64 || arm)) { return arm ? "linux-aarch64" : "linux"; }
        if ((os.startsWith("mac") || os.startsWith("darwin")) && (x64 || arm)) {
            return arm ? "mac-aarch64" : "mac";
        }
        throw new IllegalArgumentException("Unsupported platform: " + osName + " / " + architecture
                + ". Use Windows x64, macOS x64/ARM64, or Linux x64/ARM64 with Java 25.");
    }

    public static void main(String[] args) throws Exception {
        String platform = platform(System.getProperty("os.name"), System.getProperty("os.arch"));
        Path archive = Path.of(UniversalLauncher.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        Path directory = Files.createTempDirectory("unichope-");
        directory.toFile().deleteOnExit();
        var urls = new ArrayList<URL>();
        try (var jar = new JarFile(archive.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".jar") || !(name.startsWith("lib/common/")
                        || name.startsWith("lib/" + platform + "/"))) { continue; }
                Path dependency = directory.resolve(Path.of(name).getFileName().toString());
                try (var input = jar.getInputStream(entry)) {
                    Files.copy(input, dependency);
                }
                dependency.toFile().deleteOnExit();
                urls.add(dependency.toUri().toURL());
            }
        }
        if (urls.isEmpty()) { throw new IOException("No bundled dependencies found in " + archive); }
        // A platform parent prevents the outer launcher from resolving app classes.
        try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            Thread.currentThread().setContextClassLoader(loader);
            try {
                loader.loadClass("shell.Launcher").getMethod("main", String[].class)
                        .invoke(null, (Object) args);
            } catch (InvocationTargetException failure) {
                if (failure.getCause() instanceof Exception cause) { throw cause; }
                if (failure.getCause() instanceof Error cause) { throw cause; }
                throw failure;
            }
        }
    }
}
