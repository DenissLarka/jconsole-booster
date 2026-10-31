package com.druvu.jconsole.app;

import atlantafx.base.theme.Styles;
import com.druvu.jconsole.app.bookmarks.BookmarksFile;
import com.druvu.jconsole.app.bookmarks.BookmarksMenu;
import com.druvu.jconsole.app.connect.ConnectionRequest;
import com.druvu.jconsole.app.session.ConnectionController;
import com.druvu.jconsole.app.window.Icons;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * The toolbar under the menus, in the letterblade and wealth-app style: icon buttons whose tooltips carry the labels.
 * On the left the connection button, which names the JVM the app is connected to and lists the bookmarks (a click opens
 * the Connect dialog on that bookmark), then Disconnect; on the right Settings. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 31 Oct 2026
 */
final class AppToolbar {

    /** The connection button's text while there is no connection. */
    static final String CONNECT = "Connect";

    static final String NEW_CONNECTION = "New Connection…";
    static final String DISCONNECT = "Disconnect";
    static final String SETTINGS = "Settings";

    /** Lets the stylesheet tell the app's toolbar from any other. */
    static final String STYLE_CLASS = "app-toolbar";

    /** The icon buttons are squares of this size. */
    private static final double BUTTON_SIZE = 34;

    private final ConnectionController connection;
    private final BookmarksFile bookmarksFile;
    private final Runnable showSettings;
    private final MenuButton connect = new MenuButton(CONNECT, Icons.connect());

    /** @param showSettings opens the settings dialog */
    AppToolbar(ConnectionController connection, BookmarksFile bookmarksFile, Runnable showSettings) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.bookmarksFile = Objects.requireNonNull(bookmarksFile, "bookmarksFile");
        this.showSettings = Objects.requireNonNull(showSettings, "showSettings");
    }

    ToolBar build() {
        connect.setTooltip(new Tooltip("Connect to a bookmarked JVM"));
        connect.setMinHeight(BUTTON_SIZE);
        fillConnectionList();
        // the bookmarks file may have been edited since: read it each time the list opens
        connect.setOnShowing(event -> fillConnectionList());
        connection.activeProperty().addListener((observable, was, active) -> nameTheConnection(active));

        Button disconnect = iconButton(Icons.disconnect(), DISCONNECT, connection::disconnect);
        disconnect.disableProperty().bind(connection.activeProperty().not());

        // the spacer grows, pushing what follows it to the right edge
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar toolBar =
                new ToolBar(connect, disconnect, spacer, iconButton(Icons.settings(), SETTINGS, showSettings));
        toolBar.getStyleClass().add(STYLE_CLASS);
        return toolBar;
    }

    private void fillConnectionList() {
        MenuItem newConnection = new MenuItem(NEW_CONNECTION);
        newConnection.setOnAction(event -> connection.newConnection());
        connect.getItems().setAll(newConnection, new SeparatorMenuItem());
        connect.getItems().addAll(BookmarksMenu.items(bookmarksFile.groups(), connection::newConnection));
    }

    /** The button reads the connection's bookmark label, or its {@code host:port} when it is not bookmarked. */
    private void nameTheConnection(boolean active) {
        String target = connection.currentTarget();
        if (!active || target.isEmpty()) {
            connect.setText(CONNECT);
            return;
        }
        connect.setText(bookmarksFile.labelsByTarget().getOrDefault(ConnectionRequest.canonical(target), target));
    }

    private static Button iconButton(Node icon, String label, Runnable action) {
        Button button = new Button(null, icon);
        button.getStyleClass().add(Styles.BUTTON_ICON);
        button.setTooltip(new Tooltip(label));
        button.setAccessibleText(label);
        button.setMinSize(BUTTON_SIZE, BUTTON_SIZE);
        button.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        button.setOnAction(event -> action.run());
        return button;
    }
}
