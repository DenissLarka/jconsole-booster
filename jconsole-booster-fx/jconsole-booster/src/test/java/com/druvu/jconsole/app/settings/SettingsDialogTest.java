package com.druvu.jconsole.app.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.FxTestToolkit;
import com.druvu.jconsole.app.session.TabChoice;
import com.druvu.jconsole.app.session.TabSwitches;
import com.druvu.lib.fx.theme.FxTheme;
import com.druvu.lib.fx.theme.ThemeManager;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The settings dialog: Appearance applies the chosen theme, Plugins switches the tabs of the last connection. */
public class SettingsDialogTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void appearanceOffersEveryThemeAndAppliesTheChosenOne() throws IOException {
        ThemeManager themes = new ThemeManager();
        TabSwitches switches = tabSwitches();
        List<RadioButton> options = FxTestToolkit.call(() -> {
            themes.apply(FxTheme.PRIMER_LIGHT);
            return find(content(themes, switches, FXCollections.observableArrayList()), RadioButton.class);
        });

        assertThat(options)
                .extracting(RadioButton::getText)
                .containsExactly(Arrays.stream(FxTheme.values())
                        .map(FxTheme::displayName)
                        .toArray(String[]::new));
        assertThat(options.get(FxTheme.PRIMER_LIGHT.ordinal()).isSelected()).isTrue();

        FxTestToolkit.runAndWait(() -> options.get(FxTheme.DRUVU_DARK.ordinal()).fire());

        assertThat(themes.current()).isEqualTo(FxTheme.DRUVU_DARK);
    }

    @Test
    public void pluginsSwitchATabOffAndKeepTheChoice() throws IOException {
        TabSwitches switches = tabSwitches();
        ObservableList<TabChoice> tabs =
                FXCollections.observableArrayList(new TabChoice("memory", "Memory"), new TabChoice("mbeans", "MBeans"));
        List<CheckBox> boxes = FxTestToolkit.call(() -> {
            Node content = content(new ThemeManager(), switches, tabs);
            choose(content, SettingsDialog.PLUGINS);
            return find(content, CheckBox.class);
        });

        assertThat(boxes).extracting(CheckBox::getText).containsExactly("Memory", "MBeans");
        assertThat(boxes).allMatch(CheckBox::isSelected);

        FxTestToolkit.runAndWait(() -> boxes.getFirst().fire());

        assertThat(switches.isOn("memory")).isFalse();
        assertThat(switches.isOn("mbeans")).isTrue();
    }

    @Test
    public void pluginsBeforeTheFirstConnectSayHowToListTheTabs() throws IOException {
        TabSwitches switches = tabSwitches();
        Node content = FxTestToolkit.call(() -> {
            Node built = content(new ThemeManager(), switches, FXCollections.observableArrayList());
            choose(built, SettingsDialog.PLUGINS);
            return built;
        });

        assertThat(find(content, Label.class)).extracting(Label::getText).contains(SettingsDialog.NO_TABS_YET);
        assertThat(find(content, CheckBox.class)).isEmpty();
    }

    private static Node content(ThemeManager themes, TabSwitches switches, ObservableList<TabChoice> tabs) {
        return new SettingsDialog(new Stage(), themes, switches, tabs).content();
    }

    private static TabSwitches tabSwitches() throws IOException {
        return new TabSwitches(Files.createTempDirectory("settings").resolve("plugins.txt"));
    }

    private static void choose(Node content, String category) {
        @SuppressWarnings("unchecked")
        ListView<String> categories = find(content, ListView.class).getFirst();
        categories.getSelectionModel().select(category);
    }

    /** Every node of {@code type} under {@code node}, the content of a scroll pane included. */
    private static <T> List<T> find(Node node, Class<T> type) {
        List<T> found = new ArrayList<>();
        if (type.isInstance(node)) {
            found.add(type.cast(node));
        }
        if (node instanceof ScrollPane scroll && scroll.getContent() != null) {
            found.addAll(find(scroll.getContent(), type));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                found.addAll(find(child, type));
            }
        }
        return found;
    }
}
