package com.druvu.jconsole.app.window;

import atlantafx.base.theme.Styles;
import com.druvu.lib.fx.status.StatusBarModel;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * The strip at the bottom of the window: the connection's state on the left, a spinner while the app works in the
 * background (connecting, disconnecting), and the time of the last update on the right. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class StatusBar {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final double SPINNER_SIZE = 14;

    private final Label connection = new Label();
    private final Label lastUpdate = new Label();
    private final HBox node;

    StatusBar(StatusBarModel tasks) {
        ProgressIndicator busy = new ProgressIndicator();
        busy.setPrefSize(SPINNER_SIZE, SPINNER_SIZE);
        busy.visibleProperty().bind(tasks.busyProperty());
        lastUpdate.getStyleClass().add(Styles.TEXT_MUTED);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        node = new HBox(12, connection, spacer, busy, lastUpdate);
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
