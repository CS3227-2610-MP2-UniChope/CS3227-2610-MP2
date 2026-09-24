package shell;

import javafx.application.Application;

/** Separate launcher keeps classpath-based distributions usable. */
public final class Launcher {
    private Launcher() { }

    public static void main(String[] args) {
        Application.launch(UniChopeApplication.class, args);
    }
}
