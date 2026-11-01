package com.druvu.jconsole.app;

import com.druvu.jconsole.app.bookmarks.BookmarkActions;
import com.druvu.jconsole.app.bookmarks.BookmarksFile;
import com.druvu.jconsole.app.connect.ConnectDialog;
import com.druvu.jconsole.app.connect.ConnectionRequest;
import com.druvu.jconsole.app.connect.FxCertTrustPrompt;
import com.druvu.jconsole.app.connect.FxPlaintextAlert;
import com.druvu.jconsole.app.session.ConnectionController;
import com.druvu.jconsole.app.session.TabSwitches;
import com.druvu.jconsole.app.settings.SettingsDialog;
import com.druvu.jconsole.app.window.AboutBox;
import com.druvu.jconsole.app.window.ColorBand;
import com.druvu.jconsole.app.window.MainWindow;
import com.druvu.jconsole.core.bookmarks.RecentConnections;
import com.druvu.jconsole.core.connect.JMXConnectionManager;
import com.druvu.jconsole.core.home.BoosterHome;
import com.druvu.jconsole.core.launch.ArgumentParser;
import com.druvu.jconsole.core.launch.JConsoleOptions;
import com.druvu.jconsole.core.vm.ProxyClient;
import com.druvu.lib.fx.bus.FxBus;
import com.druvu.lib.fx.exec.FxExec;
import com.druvu.lib.fx.os.DesktopHooks;
import com.druvu.lib.fx.prefs.AppHome;
import com.druvu.lib.fx.prefs.Prefs;
import com.druvu.lib.fx.prefs.WindowGeometry;
import com.druvu.lib.fx.status.StatusBarModel;
import com.druvu.lib.fx.theme.ThemeManager;
import com.druvu.lib.fx.update.Updates;
import com.druvu.lib.fx.updater.github.GitHubReleases;
import com.druvu.lib.fx.util.FxThreads;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

/**
 * The JavaFX application: builds the window and its collaborators, then connects to the JVM named on the command line
 * or opens the Connect dialog. Deliberately thin: the window shows, the controller owns the connection, the toolbar and
 * the menus wire the two.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class JConsoleBoosterApp extends Application {

    /** The same home as the core's BoosterHome: ~/.druvu.com/jconsole-booster, or JCONSOLE_BOOSTER_HOME. */
    static final AppHome HOME = AppHome.of("jconsole-booster", BoosterHome.ENV_VAR);

    private static final System.Logger LOG = System.getLogger(JConsoleBoosterApp.class.getName());

    /** Cmd+, on macOS, Ctrl+, elsewhere. */
    private static final KeyCombination SETTINGS_SHORTCUT =
            new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN);

    /** Set by {@link #start}; the About hook is registered before any instance exists. */
    private static final AtomicReference<JConsoleBoosterApp> RUNNING = new AtomicReference<>();

    // One bus feeds the executor's task events to the status bar: the druvu-lib-fx wiring.
    private final FxBus bus = new FxBus();
    private final FxExec exec = new FxExec(bus);
    private final Prefs prefs = Prefs.in(HOME);
    private final ThemeManager themes = new ThemeManager(prefs);

    private StatusBarModel statusBarModel;
    private MainWindow window;
    private ConnectionController connection;

    /**
     * Starts the app. The macOS About hook is registered before the launch: that order brings AWT up before JavaFX,
     * which makes AWT the receiver of the application events macOS sends.
     */
    static void launchWith(String[] args) {
        DesktopHooks.onAbout(() -> {
            JConsoleBoosterApp app = RUNNING.get();
            if (app != null) {
                app.showAbout();
            }
        });
        // Quitting in any way closes every connection, instead of letting the server see a dropped socket.
        Runtime.getRuntime().addShutdownHook(new Thread(ProxyClient::disconnectAll, "jcb-connection-close"));
        launch(JConsoleBoosterApp.class, args);
    }

    @Override
    public void start(Stage stage) {
        FxThreads.requireFx();
        // The theme first: the kit's widgets look their colours up in it.
        themes.applyStored();
        warnIfHomesDiffer();
        // Main has parsed these arguments already; they were valid there.
        JConsoleOptions options = ArgumentParser.parse(getParameters().getRaw().toArray(String[]::new))
                .orElseThrow();

        statusBarModel = new StatusBarModel(bus);
        window = new MainWindow(stage, statusBarModel);
        ColorBand.parse(options.colorHex()).ifPresent(window::showColorBand);

        BookmarksFile bookmarksFile = new BookmarksFile(BoosterHome.connectionsFile());
        BookmarkActions bookmarkActions = new BookmarkActions(bookmarksFile, window, exec);
        TabSwitches switches = new TabSwitches(BoosterHome.pluginsFile());
        ConnectDialog connectDialog =
                new ConnectDialog(stage, RecentConnections::load, bookmarksFile::labelsByTarget, bookmarkActions::add);
        connection = new ConnectionController(
                window, connectDialog, switches, Duration.ofMillis(options.updateInterval()), exec);
        SettingsDialog settings = new SettingsDialog(stage, themes, switches, connection.tabChoices());
        Updates updates = buildUpdates(stage);
        window.setMenuBar(
                new AppMenus(updates.checkForUpdatesMenuItem(), window, getHostServices()::showDocument).build());
        window.setToolBar(new AppToolbar(connection, bookmarksFile, bookmarkActions, window, settings::show).build());
        // Settings has no menu item, only the toolbar button; the usual shortcut opens it too
        stage.getScene().getAccelerators().put(SETTINGS_SHORTCUT, settings::show);

        JMXConnectionManager.setCertTrustPrompt(new FxCertTrustPrompt(stage));
        JMXConnectionManager.setPlaintextCredentialAlert(new FxPlaintextAlert(stage));

        WindowGeometry.install(stage, prefs);
        RUNNING.set(this);
        stage.show();
        // Quiet unless a newer release is there to offer; a development run is not an installed copy and skips it.
        updates.checkAtStart();
        // After start() returns, so the window is up before a dialog or a connect begins.
        Platform.runLater(() -> firstConnection(options.urls()));
    }

    /**
     * Self-update from this repository's releases, all through the kit: the offer, the download in the status bar, the
     * SHA-256 check, the install and the restart. The dialogs belong to the main window.
     */
    private Updates buildUpdates(Stage stage) {
        return Updates.builder()
                .appName("JConsole Booster")
                .app(JConsoleBoosterApp.class)
                .source(GitHubReleases.of("DenissLarka", "jconsole-booster"))
                .owner(stage)
                .prefs(prefs)
                .exec(exec)
                .build();
    }

    @Override
    public void stop() {
        if (connection != null) {
            connection.shutdown();
        }
        if (statusBarModel != null) {
            statusBarModel.close();
        }
        exec.close();
    }

    /** The JVM named on the command line, or the Connect dialog when none was. */
    private void firstConnection(List<String> urls) {
        if (urls.isEmpty()) {
            connection.newConnection();
            return;
        }
        if (urls.size() > 1) {
            LOG.log(
                    System.Logger.Level.WARNING,
                    "JConsole Booster holds one connection: connecting to " + urls.getFirst() + ", ignoring " + urls);
        }
        connection.connect(ConnectionRequest.to(urls.getFirst()));
    }

    private void showAbout() {
        if (window != null) {
            AboutBox.show(window.window(), getHostServices()::showDocument);
        }
    }

    private void warnIfHomesDiffer() {
        if (!HOME.root().toAbsolutePath().equals(BoosterHome.root().toAbsolutePath())) {
            LOG.log(
                    System.Logger.Level.WARNING,
                    "The app's preferences live in " + HOME.root() + ", its connection files in " + BoosterHome.root());
        }
    }
}
