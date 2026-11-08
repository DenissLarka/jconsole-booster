package com.druvu.jconsole.app.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.FxTestToolkit;
import com.druvu.jconsole.app.connect.ConnectDialog;
import com.druvu.jconsole.app.window.MainWindow;
import com.druvu.lib.fx.bus.FxBus;
import com.druvu.lib.fx.exec.FxExec;
import com.druvu.lib.fx.status.StatusBarModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Covers what the connection controller does on its own, before the user asks for anything. */
public class ConnectionControllerTest {

    private final FxExec exec = new FxExec();
    private ConnectionController connection;

    @BeforeClass
    public void buildTheController() throws IOException {
        FxTestToolkit.ensureStarted();
        Path home = Files.createTempDirectory("controller");
        connection = FxTestToolkit.call(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new StatusBarModel(new FxBus()));
            ConnectDialog dialog = new ConnectDialog(stage, (owner, target) -> {});
            return new ConnectionController(
                    window, dialog, new TabSwitches(home.resolve("plugins.txt")), Duration.ofSeconds(1), exec);
        });
    }

    @AfterClass(alwaysRun = true)
    public void stopTheBackgroundThreads() {
        exec.close();
    }

    @Test
    public void aStartWithoutATargetOpensNoDialog() {
        // not waited for: a dialog would hold the call until it closes
        Platform.runLater(() -> connection.connectAtStart(List.of()));

        // queued behind the start, so a dialog it showed is open by now
        List<String> opened = FxTestToolkit.call(ConnectionControllerTest::closeTheDialogs);

        assertThat(opened).as("dialogs open after a start without a target").isEmpty();
    }

    /** The titles of the open dialogs, each closed so nothing outlives the test. */
    private static List<String> closeTheDialogs() {
        List<String> titles = new ArrayList<>();
        for (Window window : List.copyOf(Window.getWindows())) {
            // a dialog has an owner; the main window does not
            if (window instanceof Stage stage && stage.getOwner() != null && stage.isShowing()) {
                titles.add(stage.getTitle());
                stage.hide();
            }
        }
        return titles;
    }
}
