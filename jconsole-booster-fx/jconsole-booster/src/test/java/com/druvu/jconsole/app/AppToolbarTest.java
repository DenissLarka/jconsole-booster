package com.druvu.jconsole.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.bookmarks.BookmarkActions;
import com.druvu.jconsole.app.bookmarks.BookmarksFile;
import com.druvu.jconsole.app.connect.ConnectDialog;
import com.druvu.jconsole.app.session.ConnectionController;
import com.druvu.jconsole.app.session.TabSwitches;
import com.druvu.jconsole.app.window.MainWindow;
import com.druvu.lib.fx.bus.FxBus;
import com.druvu.lib.fx.exec.FxExec;
import com.druvu.lib.fx.status.StatusBarModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The toolbar without a connection: the connection button keeps one width, shows the plugs apart and lists every
 * connection command, Disconnect last; Settings sits on the right.
 */
public class AppToolbarTest {

    private final AtomicInteger settingsOpened = new AtomicInteger();
    private final FxExec exec = new FxExec();
    private ToolBar toolBar;

    @BeforeClass
    public void buildTheToolbar() throws IOException {
        FxTestToolkit.ensureStarted();
        Path home = Files.createTempDirectory("toolbar");
        Path bookmarks = home.resolve("connections.txt");
        Files.writeString(bookmarks, """
                [Prod]
                prod-1@prod-1.example.com:7091
                [Test]
                test-1@test-1.example.com:7091
                """);
        toolBar = FxTestToolkit.call(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new StatusBarModel(new FxBus()));
            ConnectDialog neverShown = new ConnectDialog(stage, (owner, target) -> {});
            ConnectionController connection = new ConnectionController(
                    window, neverShown, new TabSwitches(home.resolve("plugins.txt")), Duration.ofSeconds(1), exec);
            BookmarksFile bookmarksFile = new BookmarksFile(bookmarks);
            BookmarkActions bookmarkActions = new BookmarkActions(bookmarksFile, window, exec);
            return new AppToolbar(connection, bookmarksFile, bookmarkActions, window, settingsOpened::incrementAndGet)
                    .build();
        });
    }

    @AfterClass(alwaysRun = true)
    public void stopTheBackgroundThreads() {
        exec.close();
    }

    @Test
    public void withoutAConnectionTheButtonSaysConnectShowsThePlugsApartAndTheConnectionCommandsAreOff() {
        assertThat(connectButton().getText()).isEqualTo(AppToolbar.CONNECT);
        assertThat(connectButton().getGraphic().getStyleClass()).containsExactly("ph-plugs");
        assertThat(connectButton().getItems())
                .filteredOn(MenuItem::isDisable)
                .extracting(MenuItem::getText)
                .containsExactly(AppToolbar.ADD_BOOKMARK, AppToolbar.RECONNECT, AppToolbar.DISCONNECT);
    }

    /** The button shows the connection's bookmark name or host, where an "_" must stay text, not a shortcut mark. */
    @Test
    public void theConnectionButtonShowsUnderscores() {
        assertThat(connectButton().isMnemonicParsing()).isFalse();
    }

    @Test
    public void theConnectionButtonKeepsOneWidth() {
        assertThat(connectButton().getMinWidth()).isEqualTo(AppToolbar.CONNECTION_WIDTH);
        assertThat(connectButton().getPrefWidth()).isEqualTo(AppToolbar.CONNECTION_WIDTH);
        assertThat(connectButton().getMaxWidth()).isEqualTo(AppToolbar.CONNECTION_WIDTH);
    }

    @Test
    public void theListHoldsEveryConnectionCommandWithDisconnectLast() {
        List<MenuItem> items = connectButton().getItems();

        assertThat(items)
                .extracting(item -> item instanceof SeparatorMenuItem ? "---" : item.getText())
                .containsExactly(
                        AppToolbar.NEW_CONNECTION,
                        "---",
                        "Prod",
                        "Test",
                        "---",
                        AppToolbar.ADD_BOOKMARK,
                        AppToolbar.OPEN_BOOKMARKS,
                        "---",
                        AppToolbar.RECONNECT,
                        AppToolbar.DISCONNECT);
        assertThat(items.getFirst().getAccelerator())
                .isEqualTo(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
    }

    @Test
    public void settingsSitsOnTheRightAndOpensTheDialog() {
        Button settings = button(AppToolbar.SETTINGS);

        FxTestToolkit.runAndWait(settings::fire);

        assertThat(toolBar.getItems()).hasSize(3).first().isSameAs(connectButton());
        assertThat(toolBar.getItems().getLast()).isSameAs(settings);
        assertThat(settingsOpened).hasValue(1);
    }

    private MenuButton connectButton() {
        return (MenuButton) toolBar.getItems().getFirst();
    }

    private Button button(String label) {
        return toolBar.getItems().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> label.equals(button.getAccessibleText()))
                .findFirst()
                .orElseThrow();
    }
}
