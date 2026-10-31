package com.druvu.jconsole.app;

import com.druvu.jconsole.app.bookmarks.BookmarkActions;
import com.druvu.jconsole.app.bookmarks.BookmarksFile;
import com.druvu.jconsole.app.bookmarks.BookmarksMenu;
import com.druvu.jconsole.app.session.ConnectionController;
import com.druvu.jconsole.app.window.AboutBox;
import com.druvu.jconsole.app.window.MainWindow;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * The app's menus: Connection (connect, bookmarks, disconnect, exit), View (settings: the theme and the tabs) and Help
 * (guide, feedback, About). About sits in Help on every system, macOS included. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class AppMenus {

    private final ConnectionController connection;
    private final BookmarksFile bookmarksFile;
    private final BookmarkActions bookmarkActions;
    private final Runnable showSettings;
    private final MainWindow window;
    private final Consumer<String> openLink;
    private final Menu bookmarksMenu = new Menu("Bookmarks");

    /**
     * @param showSettings opens the settings dialog
     * @param openLink opens an address in the browser
     */
    AppMenus(
            ConnectionController connection,
            BookmarksFile bookmarksFile,
            BookmarkActions bookmarkActions,
            Runnable showSettings,
            MainWindow window,
            Consumer<String> openLink) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.bookmarksFile = Objects.requireNonNull(bookmarksFile, "bookmarksFile");
        this.bookmarkActions = Objects.requireNonNull(bookmarkActions, "bookmarkActions");
        this.showSettings = Objects.requireNonNull(showSettings, "showSettings");
        this.window = Objects.requireNonNull(window, "window");
        this.openLink = Objects.requireNonNull(openLink, "openLink");
    }

    MenuBar build() {
        return new MenuBar(connectionMenu(), viewMenu(), helpMenu());
    }

    /** Reads the bookmarks file again: a bookmark may have been added or the file edited since. */
    private void refreshBookmarks() {
        BookmarksMenu.fill(bookmarksMenu, bookmarksFile.groups(), connection::newConnection);
    }

    private Menu connectionMenu() {
        MenuItem newConnection = item("New Connection…", connection::newConnection);
        newConnection.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));

        refreshBookmarks();
        // the file may have been edited since: read it each time the menu opens
        bookmarksMenu.setOnShowing(event -> refreshBookmarks());
        MenuItem addBookmark = item(
                "Add current connection to bookmarks…",
                () -> bookmarkActions.add(window.window(), connection.currentTarget()));
        addBookmark.disableProperty().bind(connection.activeProperty().not());
        MenuItem openBookmarks = item("Open bookmarks file", bookmarkActions::openFile);

        MenuItem disconnect = item("Disconnect", connection::disconnect);
        disconnect.disableProperty().bind(connection.activeProperty().not());
        MenuItem reconnect = item("Reconnect", connection::reconnect);
        reconnect.disableProperty().bind(connection.reconnectableProperty().not());

        return new Menu(
                "Connection",
                null,
                newConnection,
                new SeparatorMenuItem(),
                bookmarksMenu,
                addBookmark,
                openBookmarks,
                new SeparatorMenuItem(),
                disconnect,
                reconnect,
                new SeparatorMenuItem(),
                item("Exit", Platform::exit));
    }

    private Menu viewMenu() {
        MenuItem settings = item("Settings…", showSettings);
        settings.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));
        return new Menu("View", null, settings);
    }

    private Menu helpMenu() {
        return new Menu(
                "Help",
                null,
                item("User Guide", () -> openLink.accept(AboutBox.USER_GUIDE)),
                item("Feedback", () -> openLink.accept(AboutBox.FEEDBACK)),
                new SeparatorMenuItem(),
                item("About JConsole Booster", () -> AboutBox.show(window.window(), openLink)));
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
    }
}
