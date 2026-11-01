package com.druvu.jconsole.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.window.MainWindow;
import com.druvu.lib.fx.bus.FxBus;
import com.druvu.lib.fx.status.StatusBarModel;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Stage;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The menu bar holds Help only: the guide, feedback, the update check and About. */
public class AppMenusTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void theMenuBarHoldsHelpOnly() {
        MenuItem checkForUpdates = new MenuItem("Check for Updates...");

        MenuBar menuBar = FxTestToolkit.call(() -> {
            MainWindow window = new MainWindow(new Stage(), new StatusBarModel(new FxBus()));
            return new AppMenus(checkForUpdates, window, link -> {}).build();
        });

        assertThat(menuBar.getMenus()).extracting(Menu::getText).containsExactly(AppMenus.HELP);
        assertThat(menuBar.getMenus().getFirst().getItems())
                .extracting(item -> item instanceof SeparatorMenuItem ? "---" : item.getText())
                .containsExactly("User Guide", "Feedback", "---", "Check for Updates...", "About JConsole Booster");
        assertThat(menuBar.getMenus().getFirst().getItems()).contains(checkForUpdates);
    }
}
