# UniChope

Java desktop consultation booking system.

## Run and test

Use a valid JDK 17 or later to launch the checked-in Gradle 9.1.0 wrapper. Gradle
resolves a Java 25 toolchain automatically on first use (internet required). The
application and compiled classes require Java 25. Set JAVA_HOME to an existing JDK
if your environment has a stale value.

Windows:

```powershell
.\gradlew.bat run
.\gradlew.bat test
.\gradlew.bat uiTest
.\gradlew.bat installDist
```

macOS/Linux: use `./gradlew` with the same tasks. `uiTest` requires a graphical desktop;
`test` excludes desktop tests and is suitable for headless CI.

The login screen is explicitly a **demo account selector**, with one active sample
account per role. It is not secure authentication. Data is stored in SQLite at
`data/unichope.db` and remains after restart. Admin opens
user/module/assignment management, booking, and statistics tabs; Tutor opens the
consultation workspace; Student can book and cancel consultations. All roles can sign out.
The demo seeds active modules from the NUS CS AY2026/27 curriculum, while preserving
modules already stored in the database.

## Single cross-platform JAR

Build with `./gradlew universalJar` (macOS/Linux) or
`.\gradlew.bat universalJar` (Windows). The output is **`build/libs/UniChope.jar`**;
`build` and `assemble` also create it. Copy just this file to the target computer.

With **Java 25** installed, run from a writable folder:

```text
java -jar UniChope.jar
```

The same JAR bundles JavaFX, SQLite, and Argon2 dependencies for Windows x64,
macOS Intel/Apple Silicon, and Linux x64/ARM64. The Java installation must match
one of these architectures. A graphical desktop is required; Linux also needs
the system libraries required by JavaFX (including GTK 3). Java itself is not bundled.
The launcher extracts the selected dependencies into the system temporary directory
and schedules them for deletion on exit. Data remains in `data/unichope.db` relative
to the folder from which you launch the app. No Gradle or separate JavaFX installation
is needed on the target computer.

`installDist` creates `build/install/UniChope/` containing launchers and only the
current platform's runtime dependencies, for local development.

The application uses one SQLite-backed repository bundle for every role. Tests use
temporary SQLite databases; repository interfaces keep storage separate from role
services and JavaFX views.

See [User Guide](docs/UserGuide.md) for available actions and [Developer Guide](docs/DeveloperGuide.md) for design, testing, logging, and persistence.
