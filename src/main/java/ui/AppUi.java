package ui;

import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Shared visual language for the desktop workspaces. */
public final class AppUi {
    private static final String STYLESHEET = Objects.requireNonNull(
            AppUi.class.getResource("/ui/unichope.css"), "Application stylesheet").toExternalForm();

    private AppUi() { }

    public static void theme(Parent root) {
        if (!root.getStylesheets().contains(STYLESHEET)) { root.getStylesheets().add(STYLESHEET); }
        root.getStyleClass().add("app-root");
    }

    public static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setWrapText(true);
        return label;
    }

    public static HBox brand() {
        Label mark = label("\u25b3", "brand-mark");
        HBox brand = new HBox(10, mark, label("UniChope", "brand-name"));
        brand.setAlignment(Pos.CENTER_LEFT);
        return brand;
    }

    public static void workspace(BorderPane root, Label heading, String description,
                                 Node refresh, Node logout, Node tabs, Label message) {
        theme(root);
        root.getStyleClass().add("workspace-root");
        heading.getStyleClass().add("workspace-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox navigation = new HBox(12, brand(), spacer, refresh, logout);
        navigation.getStyleClass().add("workspace-navigation");
        navigation.setAlignment(Pos.CENTER_LEFT);
        VBox header = new VBox(18, navigation, new VBox(5, heading, label(description, "muted")));
        header.getStyleClass().add("workspace-header");
        root.setTop(header);
        root.setCenter(tabs);
        message.getStyleClass().add("feedback");
        message.setMinHeight(24);
        message.setWrapText(true);
        root.setBottom(message);
        root.heightProperty().addListener((observable, before, height) -> {
            boolean compact = height.doubleValue() < 720;
            if (compact && !root.getStyleClass().contains("compact")) { root.getStyleClass().add("compact"); }
            if (!compact) { root.getStyleClass().remove("compact"); }
            BorderPane.setMargin(tabs, new Insets(compact ? 12 : 22, 0, 12, 0));
        });
    }

    public static VBox field(String name, Control control) {
        Label label = label(name, "field-label");
        label.setLabelFor(control);
        control.setAccessibleText(name);
        control.setPrefWidth(180);
        VBox field = new VBox(7, label, control);
        field.setFillWidth(true);
        return field;
    }

    public static VBox section(String title, String detail, Node controls, TableView<?> table, Node... actions) {
        Label count = label("", "result-count");
        count.textProperty().bind(Bindings.createStringBinding(() -> table.getItems().size()
                + (table.getItems().size() == 1 ? " result" : " results"), table.getItems()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(12, count, spacer);
        toolbar.getChildren().addAll(actions);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(16, new VBox(5, label(title, "section-title"), label(detail, "muted")),
                controls, toolbar, table);
        content.getStyleClass().add("content-section");
        table.setMinHeight(80);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    public static FlowPane filters(Node... controls) {
        FlowPane filters = new FlowPane(12, 12, controls);
        filters.setRowValignment(VPos.BOTTOM);
        return filters;
    }

    public static Node empty(String title, String detail) {
        VBox empty = new VBox(10, label("\u25c7", "empty-symbol"), label(title, "empty-title"),
                label(detail, "muted"));
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(24));
        return empty;
    }

    public static void primary(Node action) { action.getStyleClass().add("primary-action"); }
    public static void danger(Node action) { action.getStyleClass().add("danger-action"); }
}
