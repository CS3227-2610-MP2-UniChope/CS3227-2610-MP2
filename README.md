# UniChope

Java desktop consultation booking system. This branch implements the shared foundation;
student, tutor, and admin business features are not implemented yet.

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
account per role. It is not secure authentication. Data is in memory and resets on
restart. Each role currently opens a placeholder workspace with a sign-out button.

`installDist` creates `build/install/UniChope/` containing launchers and the current
platform's runtime dependencies. This is a development distribution, not the final
cross-platform release JAR required by the assignment.

The in-memory repositories are temporary development storage. Person B will replace
the application's storage with a tested persistence implementation before release;
the repository interfaces remain, and the fakes can continue to support tests.
