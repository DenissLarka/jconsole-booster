package com.druvu.jconsole.app;

import com.druvu.jconsole.app.window.AboutBox;
import com.druvu.jconsole.app.window.MainWindow;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * The app's one menu, Help: guide, feedback, the update check, About. Everything else sits in the toolbar: the
 * connection commands in the connection list ({@link AppToolbar}), Settings on the right. On macOS the system adds the
 * application menu (Quit among it); elsewhere the window's close button quits. About sits in Help on every system. FX
 * thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class AppMenus {

    static final String HELP = "Help";

    private final MenuItem checkForUpdates;
    private final MainWindow window;
    private final Consumer<String> openLink;

    /**
     * @param checkForUpdates the self-update's own item ({@code Updates.checkForUpdatesMenuItem()})
     * @param openLink opens an address in the browser
     */
    AppMenus(MenuItem checkForUpdates, MainWindow window, Consumer<String> openLink) {
        this.checkForUpdates = Objects.requireNonNull(checkForUpdates, "checkForUpdates");
        this.window = Objects.requireNonNull(window, "window");
        this.openLink = Objects.requireNonNull(openLink, "openLink");
    }

    MenuBar build() {
        return new MenuBar(helpMenu());
    }

    private Menu helpMenu() {
        return new Menu(
                HELP,
                null,
                item("User Guide", () -> openLink.accept(AboutBox.USER_GUIDE)),
                item("Feedback", () -> openLink.accept(AboutBox.FEEDBACK)),
                new SeparatorMenuItem(),
                // the user's own check: it reports "up to date" and failures too, and says so in a development run
                checkForUpdates,
                item("About JConsole Booster", () -> AboutBox.show(window.window(), openLink)));
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
    }
}
