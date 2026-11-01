package com.druvu.jconsole.app.window;

import atlantafx.base.theme.Styles;
import com.druvu.lib.fx.status.StatusBarModel;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * The strip at the bottom of the window: the connection's state on the left, a spinner while the app works in the
 * background (connecting, disconnecting), the bar of a task that reports its progress (an update's download), and the
 * time of the last update on the right. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class StatusBar {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final double SPINNER_SIZE = 14;
    private static final double PROGRESS_WIDTH = 140;

    private final Label connection = new Label();
    private final Label lastUpdate = new Label();
    private final HBox node;

    StatusBar(StatusBarModel tasks) {
        ProgressIndicator busy = new ProgressIndicator();
        busy.setPrefSize(SPINNER_SIZE, SPINNER_SIZE);
        busy.visibleProperty().bind(tasks.busyProperty());
        // shown only while a task reports how far it is, and then it takes its place in the strip
        ProgressBar progress = new ProgressBar();
        progress.setPrefWidth(PROGRESS_WIDTH);
        progress.progressProperty().bind(tasks.progressProperty());
        progress.visibleProperty().bind(tasks.progressingProperty());
        progress.managedProperty().bind(progress.visibleProperty());
        lastUpdate.getStyleClass().add(Styles.TEXT_MUTED);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        node = new HBox(12, connection, spacer, progress, busy, lastUpdate);
        node.setAlignment(Pos.CENTER_LEFT);
        node.setPadding(new Insets(4, 10, 4, 10));
    }

    Node node() {
        return node;
    }

    void showConnection(String state) {
        connection.setText(state);
    }

    void showLastUpdate(LocalTime at) {
        lastUpdate.setText("Last update " + CLOCK.format(at));
    }

    void clearLastUpdate() {
        lastUpdate.setText("");
    }

    String connectionText() {
        return connection.getText();
    }

    String lastUpdateText() {
        return lastUpdate.getText();
    }
}
