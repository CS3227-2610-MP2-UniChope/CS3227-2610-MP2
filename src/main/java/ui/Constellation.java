package ui;

import java.util.Random;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/** Lightweight, deterministic decorative artwork that scales with the login layout. */
public final class Constellation extends Region {
    private final Canvas canvas = new Canvas();

    public Constellation() {
        getChildren().add(canvas);
        setPrefSize(460, 170);
        setMinSize(0, 90);
        setMouseTransparent(true);
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        canvas.setWidth(width);
        canvas.setHeight(height);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, width, height);
        Random random = new Random(3227);
        Color[] colors = {Color.web("#2563EB"), Color.web("#1E3A8A"), Color.web("#60A5FA"), Color.web("#93C5FD")};
        for (int index = 0; index < 190; index++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.45 + random.nextDouble() * 0.55;
            double x = width * (0.46 + Math.cos(angle) * radius * 0.43);
            double y = height * (0.5 + Math.sin(angle) * radius * 0.4);
            double size = 1.5 + random.nextDouble() * 3.2;
            g.setGlobalAlpha(0.25 + random.nextDouble() * 0.7);
            g.setStroke(colors[index % colors.length]);
            g.setLineWidth(0.9);
            g.strokePolygon(new double[] {x, x + size, x - size},
                    new double[] {y - size, y + size, y + size}, 3);
        }
        g.setGlobalAlpha(1);
    }
}
