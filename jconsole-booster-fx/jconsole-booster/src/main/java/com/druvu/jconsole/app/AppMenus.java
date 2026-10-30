package com.druvu.jconsole.app;

import com.druvu.jconsole.app.bookmarks.BookmarkActions;
import com.druvu.jconsole.app.bookmarks.BookmarksFile;
import com.druvu.jconsole.app.bookmarks.BookmarksMenu;
import com.druvu.jconsole.app.session.ConnectionController;
import com.druvu.jconsole.app.session.TabChoice;
import com.druvu.jconsole.app.session.TabSwitches;
import com.druvu.jconsole.app.window.AboutBox;
import com.druvu.jconsole.app.window.MainWindow;
import com.druvu.lib.fx.theme.FxTheme;
import com.druvu.lib.fx.theme.ThemeManager;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * The app's menus: Connection (connect, bookmarks, disconnect, exit), View (theme, tabs) and Help (guide, feedback,
 * About). About sits in Help on every system, macOS included. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class AppMenus {

    private static final String NO_TABS_YET = "(connect to list the tabs)";

    private final ConnectionController connection;
    private final BookmarksFile bookmarksFile;
    private final BookmarkActions bookmarkActions;
    private final ThemeManager themes;
    private final TabSwitches switches;
    private final MainWindow window;
    private final Consumer<String> openLink;
    private final Menu bookmarksMenu = new Menu("Bookmarks");
    private final Menu tabsMenu = new Menu("Tabs");

    /** @param openLink opens an address in the browser */
    AppMenus(
            ConnectionController connection,
            BookmarksFile bookmarksFile,
            BookmarkActions bookmarkActions,
            ThemeManager themes,
            TabSwitches switches,
            MainWindow window,
            Consumer<String> openLink) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.bookmarksFile = Objects.requireNonNull(bookmarksFile, "bookmarksFile");
        this.bookmarkActions = Objects.requireNonNull(bookmarkActions, "bookmarkActions");
        this.themes = Objects.requireNonNull(themes, "themes");
        this.switches = Objects.requireNonNull(switches, "switches");
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
        fillTabsMenu();
        connection.tabChoices().addListener((ListChangeListener<TabChoice>) change -> fillTabsMenu());
        return new Menu("View", null, themeMenu(), tabsMenu);
    }

    private Menu themeMenu() {
        Menu menu = new Menu("Theme");
        ToggleGroup choice = new ToggleGroup();
        for (FxTheme theme : FxTheme.values()) {
            RadioMenuItem item = new RadioMenuItem(theme.displayName());
            item.setToggleGroup(choice);
            item.setSelected(theme == themes.current());
            item.setOnAction(event -> themes.apply(theme));
            menu.getItems().add(item);
        }
        return menu;
    }

    /** One switch per tab of the last connection; a change counts from the next connect. */
    private void fillTabsMenu() {
        tabsMenu.getItems().clear();
        if (connection.tabChoices().isEmpty()) {
            MenuItem none = new MenuItem(NO_TABS_YET);
            none.setDisable(true);
            tabsMenu.getItems().add(none);
            return;
        }
        for (TabChoice tab : connection.tabChoices()) {
            CheckMenuItem item = new CheckMenuItem(tab.title());
            item.setSelected(switches.isOn(tab.id()));
            item.setOnAction(event -> {
                switches.set(tab.id(), item.isSelected());
                window.notice(tab.title() + (item.isSelected() ? " on" : " off") + " from the next connect");
            });
            tabsMenu.getItems().add(item);
        }
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
