package com.druvu.jconsole.app;

import com.druvu.jconsole.app.bookmarks.BookmarkActions;
import com.druvu.jconsole.app.bookmarks.BookmarksFile;
import com.druvu.jconsole.app.connect.ConnectDialog;
import com.druvu.jconsole.app.connect.ConnectionRequest;
import com.druvu.jconsole.app.connect.FxCertTrustPrompt;
import com.druvu.jconsole.app.connect.FxPlaintextAlert;
import com.druvu.jconsole.app.session.ConnectionController;
import com.druvu.jconsole.app.session.TabSwitches;
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
import com.druvu.lib.fx.util.FxThreads;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * The JavaFX application: builds the window and its collaborators, then connects to the JVM named on the command line
 * or opens the Connect dialog. Deliberately thin: the window shows, the controller owns the connection, the menus wire
 * the two.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@Slf4j
public final class JConsoleBoosterApp extends Application {

    /** Set by {@link #start}; the About hook is registered before any instance exists. */
    private static final AtomicReference<JConsoleBoosterApp> RUNNING = new AtomicReference<>();

    // One bus feeds the executor's task events to the status bar: the druvu-lib-fx wiring.
    private final FxBus bus = new FxBus();
    private final FxExec exec = new FxExec(bus);
    // The same home as the core's BoosterHome: ~/.druvu.com/jconsole-booster, or JCONSOLE_BOOSTER_HOME.
    private final AppHome home = AppHome.of("jconsole-booster", BoosterHome.ENV_VAR);
    private final Prefs prefs = Prefs.in(home);
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
        AppMenus menus = new AppMenus(
                connection, bookmarksFile, bookmarkActions, themes, switches, window, getHostServices()::showDocument);
        window.setMenuBar(menus.build());

        JMXConnectionManager.setCertTrustPrompt(new FxCertTrustPrompt(stage));
        JMXConnectionManager.setPlaintextCredentialAlert(new FxPlaintextAlert(stage));

        WindowGeometry.install(stage, prefs);
        RUNNING.set(this);
        stage.show();
        // After start() returns, so the window is up before a dialog or a connect begins.
        Platform.runLater(() -> firstConnection(options.urls()));
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
            log.warn("JConsole Booster holds one connection: connecting to {}, ignoring {}", urls.getFirst(), urls);
        }
        connection.connect(ConnectionRequest.to(urls.getFirst()));
    }

    private void showAbout() {
        if (window != null) {
            AboutBox.show(window.window(), getHostServices()::showDocument);
        }
    }

    private void warnIfHomesDiffer() {
        if (!home.root().toAbsolutePath().equals(BoosterHome.root().toAbsolutePath())) {
            log.warn("The app's preferences live in {}, its connection files in {}", home.root(), BoosterHome.root());
        }
    }
}
