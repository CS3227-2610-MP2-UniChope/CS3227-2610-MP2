package support;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javax.imageio.ImageIO;

/** Screenshot evidence for desktop smoke tests; use on the JavaFX thread. */
public final class UiSnapshots {
    private UiSnapshots() { }

    public static void save(Parent root, String name) throws Exception {
        root.applyCss();
        root.layout();
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setViewport(new Rectangle2D(0, 0, root.getLayoutBounds().getWidth(), root.getLayoutBounds().getHeight()));
        var image = root.snapshot(parameters, null);
        BufferedImage output = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < output.getHeight(); y++) {
            for (int x = 0; x < output.getWidth(); x++) {
                output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        Path directory = Path.of("build", "reports", "design-snapshots");
        Files.createDirectories(directory);
        ImageIO.write(output, "png", directory.resolve(name + ".png").toFile());
    }
}
