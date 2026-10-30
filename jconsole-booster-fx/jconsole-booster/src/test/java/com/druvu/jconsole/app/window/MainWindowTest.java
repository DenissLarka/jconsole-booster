package com.druvu.jconsole.app.window;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.FxTestToolkit;
import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.lib.fx.bus.FxBus;
import com.druvu.lib.fx.status.StatusBarModel;
import java.time.LocalTime;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Covers the window without showing it: title and status per state, tabs shown in order with one selected, cleared. */
public class MainWindowTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void theTitleFollowsTheConnection() {
        List<String> titles = FxTestToolkit.call(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new StatusBarModel(new FxBus()));
            String initial = stage.getTitle();
            window.showConnected("admin@h:1", "Connected to admin@h:1");
            String connected = stage.getTitle();
            window.showNotConnected("Connection to admin@h:1 lost");
            return List.of(initial, connected, stage.getTitle());
        });

        assertThat(titles)
                .containsExactly(
                        "JConsole Booster (disconnected)",
                        "JConsole Booster — admin@h:1",
                        "JConsole Booster (disconnected)");
    }

    @Test
    public void tabsAreShownInOrderWithOneSelectedAndCleared() {
        List<Object> seen = FxTestToolkit.call(() -> {
            Stage stage = new Stage();
            MainWindow window = new MainWindow(stage, new StatusBarModel(new FxBus()));
            TabPane tabPane = (TabPane) stage.getScene().getRoot().lookup(".tab-pane");
            LabelTab memory = new LabelTab("memory");
            LabelTab mbeans = new LabelTab("mbeans");
            window.showTabs(List.of(memory, mbeans), mbeans);
            List<String> titles = tabPane.getTabs().stream().map(Tab::getText).toList();
            String selected = tabPane.getSelectionModel().getSelectedItem().getText();
            window.disableTab(memory);
            boolean memoryDisabled = tabPane.getTabs().getFirst().isDisable();
            window.showLastUpdate(LocalTime.of(12, 34, 56));
            window.clearTabs();
            return List.of(titles, selected, memoryDisabled, tabPane.getTabs().size(), tabPane.isVisible());
        });

        assertThat(seen).containsExactly(List.of("memory", "mbeans"), "mbeans", true, 0, false);
    }

    /** A tab that shows its id in a label. */
    private static final class LabelTab implements ConnectionTab {

        private final String id;

        LabelTab(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String title() {
            return id;
        }

        @Override
        public int place() {
            return 0;
        }

        @Override
        public Node content() {
            return new Label(id);
        }

        @Override
        public Runnable refresh() {
            return () -> {};
        }
    }
}
