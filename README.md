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
`~/.unichope/unichope.db` and remains after restart. Admin opens
user/module/assignment management, booking, and statistics tabs; Tutor opens the
consultation workspace. The Student workspace remains incomplete. All roles can sign out.

`installDist` creates `build/install/UniChope/` containing launchers and the current
platform's runtime dependencies. This is a development distribution, not the final
cross-platform release JAR required by the assignment.

The application uses one SQLite-backed repository bundle for every role. The
in-memory repositories remain as isolated test fakes; repository interfaces keep
storage separate from role services and JavaFX views.

See [User Guide](docs/UserGuide.md) for available actions and [Developer Guide](docs/DeveloperGuide.md) for design, testing, logging, and persistence.
