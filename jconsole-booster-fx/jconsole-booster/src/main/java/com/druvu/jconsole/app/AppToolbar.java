package com.druvu.jconsole.app;

import atlantafx.base.theme.Styles;
import com.druvu.jconsole.app.bookmarks.BookmarkActions;
import com.druvu.jconsole.app.bookmarks.BookmarksFile;
import com.druvu.jconsole.app.bookmarks.BookmarksMenu;
import com.druvu.jconsole.app.connect.ConnectionRequest;
import com.druvu.jconsole.app.session.ConnectionController;
import com.druvu.jconsole.app.window.Icons;
import com.druvu.jconsole.app.window.MainWindow;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * The toolbar under the menus, in the letterblade and wealth-app style: icon buttons whose tooltips carry the labels.
 * On the left the connection button, one width whatever it names: its icon shows whether the connection is open, its
 * text names the JVM, and its list holds every connection command: New Connection, the bookmarks (a click opens the
 * Connect dialog on that bookmark), adding the connection to them, the bookmarks file, Reconnect and, at the bottom,
 * Disconnect. On the right Settings. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 31 Oct 2026
 */
final class AppToolbar {

    /** The connection button's text while there is no connection. */
    static final String CONNECT = "Connect";

    static final String NEW_CONNECTION = "New Connection…";
    static final String ADD_BOOKMARK = "Add current connection to bookmarks…";
    static final String OPEN_BOOKMARKS = "Open bookmarks file";
    static final String RECONNECT = "Reconnect";
    static final String DISCONNECT = "Disconnect";
    static final String SETTINGS = "Settings";

    /** Lets the stylesheet tell the app's toolbar from any other. */
    static final String STYLE_CLASS = "app-toolbar";

    /** The connection button's width: room for a long bookmark label, and fixed, so the toolbar never shifts. */
    static final double CONNECTION_WIDTH = 320;

    /** The icon buttons are squares of this size. */
    private static final double BUTTON_SIZE = 34;

    private final ConnectionController connection;
    private final BookmarksFile bookmarksFile;
    private final BookmarkActions bookmarkActions;
    private final MainWindow window;
    private final Runnable showSettings;
    private final MenuButton connect = new MenuButton(CONNECT);
    private final Tooltip connectTooltip = new Tooltip();

    /**
     * @param window the owner of the add-bookmark dialog
     * @param showSettings opens the settings dialog
     */
    AppToolbar(
            ConnectionController connection,
            BookmarksFile bookmarksFile,
            BookmarkActions bookmarkActions,
            MainWindow window,
            Runnable showSettings) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.bookmarksFile = Objects.requireNonNull(bookmarksFile, "bookmarksFile");
        this.bookmarkActions = Objects.requireNonNull(bookmarkActions, "bookmarkActions");
        this.window = Objects.requireNonNull(window, "window");
        this.showSettings = Objects.requireNonNull(showSettings, "showSettings");
    }

    ToolBar build() {
        connect.setMinWidth(CONNECTION_WIDTH);
        connect.setPrefWidth(CONNECTION_WIDTH);
        connect.setMaxWidth(CONNECTION_WIDTH);
        connect.setMinHeight(BUTTON_SIZE);
        // the text starts at the left, as in a field; a name too long ends in "…", the tooltip has it whole
        connect.setAlignment(Pos.CENTER_LEFT);
        // it shows a bookmark's name or a host: an "_" in them is text, not the mark of a keyboard shortcut
        connect.setMnemonicParsing(false);
        connect.setTooltip(connectTooltip);
        fillConnectionList();
        // the bookmarks file may have been edited since: read it each time the list opens
        connect.setOnShowing(event -> fillConnectionList());
        connection.activeProperty().addListener((observable, was, active) -> showTheConnection());
        connection.connectedProperty().addListener((observable, was, connected) -> showTheConnection());
        showTheConnection();

        // the spacer grows, pushing what follows it to the right edge
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar toolBar = new ToolBar(connect, spacer, iconButton(Icons.settings(), SETTINGS, showSettings));
        toolBar.getStyleClass().add(STYLE_CLASS);
        return toolBar;
    }

    private void fillConnectionList() {
        MenuItem newConnection = item(NEW_CONNECTION, connection::newConnection);
        newConnection.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        MenuItem addBookmark =
                item(ADD_BOOKMARK, () -> bookmarkActions.add(window.window(), connection.currentTarget()));
        addBookmark.disableProperty().bind(connection.activeProperty().not());
        MenuItem reconnect = item(RECONNECT, connection::reconnect);
        reconnect.disableProperty().bind(connection.reconnectableProperty().not());
        MenuItem disconnect = item(DISCONNECT, connection::disconnect);
        disconnect.disableProperty().bind(connection.activeProperty().not());

        connect.getItems().setAll(newConnection, new SeparatorMenuItem());
        connect.getItems().addAll(BookmarksMenu.items(bookmarksFile.groups(), connection::newConnection));
        connect.getItems()
                .addAll(
                        new SeparatorMenuItem(),
                        addBookmark,
                        item(OPEN_BOOKMARKS, bookmarkActions::openFile),
                        new SeparatorMenuItem(),
                        reconnect,
                        disconnect);
    }

    /**
     * The icon shows whether the connection is open. The text names the connection by its bookmark label, or by its
     * {@code host:port} when it is not bookmarked; the tooltip names it whole.
     */
    private void showTheConnection() {
        connect.setGraphic(connection.connectedProperty().get() ? Icons.connected() : Icons.disconnected());
        String target = connection.currentTarget();
        if (!connection.activeProperty().get() || target.isEmpty()) {
            connect.setText(CONNECT);
            connectTooltip.setText("Connect to a JVM: a new connection or a bookmark");
            return;
        }
        String name = bookmarksFile.labelsByTarget().getOrDefault(ConnectionRequest.canonical(target), target);
        connect.setText(name);
        connectTooltip.setText(name.equals(target) ? target : name + " (" + target + ")");
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
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
