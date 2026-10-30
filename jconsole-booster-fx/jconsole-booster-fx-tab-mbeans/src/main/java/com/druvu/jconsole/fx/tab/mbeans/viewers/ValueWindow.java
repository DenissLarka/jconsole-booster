package com.druvu.jconsole.fx.tab.mbeans.viewers;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.TabHost;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * A window showing one value: an operation's return value, or an attribute value too structured for its table cell.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class ValueWindow {

    private ValueWindow() {}

    /** Opens the window next to the app's; structured values in the viewer, anything else as text. */
    public static void show(TabHost host, String title, Object value) {
        Stage stage = new Stage();
        stage.initOwner(host.window());
        stage.setTitle(title);
        Button close = new Button(Messages.CLOSE);
        close.setOnAction(event -> stage.close());
        HBox buttons = new HBox(close);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(6, 0, 0, 0));
        BorderPane root = new BorderPane(content(host, value));
        root.setBottom(buttons);
        root.setPadding(new Insets(8));
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    }

    /** The viewer for a structured value, else the value as text. */
    public static Node content(TabHost host, Object value) {
        return ValueLevel.of(value)
                .map(level -> new ValueViewer(level, host).node())
                .orElseGet(() -> new TextResult(String.valueOf(value)).node());
    }
}
