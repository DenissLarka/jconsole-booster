package com.druvu.jconsole.app.settings;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.druvu.jconsole.app.session.TabChoice;
import com.druvu.jconsole.app.session.TabSwitches;
import com.druvu.lib.fx.theme.FxTheme;
import com.druvu.lib.fx.theme.ThemeManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * The settings dialog: the categories on the left, the chosen one's page on the right. Appearance chooses the theme,
 * which applies at once; Plugins switches the tabs found at the last connect on or off, from the next connect. Each
 * setting is kept by its owner, the {@link ThemeManager} and the {@link TabSwitches}: the dialog only shows and changes
 * them, so it is built anew each time it opens. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 31 Oct 2026
 */
public final class SettingsDialog {

    static final String APPEARANCE = "Appearance";
    static final String PLUGINS = "Plugins";
    static final String NO_TABS_YET = "Connect once to list the tabs.";

    private static final String PLUGINS_NOTE =
            "The tabs found at the last connect. A tab switched off is left out from the next connect on.";
    private static final double CATEGORIES_WIDTH = 150;
    private static final double WIDTH = 580;
    private static final double HEIGHT = 340;

    private final Window owner;
    private final ThemeManager themes;
    private final TabSwitches switches;
    private final ObservableList<TabChoice> tabs;

    /** @param tabs the tabs of the last connection, empty before the first */
    public SettingsDialog(Window owner, ThemeManager themes, TabSwitches switches, ObservableList<TabChoice> tabs) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.themes = Objects.requireNonNull(themes, "themes");
        this.switches = Objects.requireNonNull(switches, "switches");
        this.tabs = Objects.requireNonNull(tabs, "tabs");
    }

    /** Opens the dialog on Appearance and waits until it is closed. */
    public void show() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Settings");
        dialog.getDialogPane().setContent(content());
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /** The category list and the chosen category's page. */
    Node content() {
        ListView<String> categories = new ListView<>(FXCollections.observableArrayList(APPEARANCE, PLUGINS));
        categories.setPrefWidth(CATEGORIES_WIDTH);
        // a page's wrapping text must not squeeze the list
        categories.setMinWidth(Region.USE_PREF_SIZE);
        StackPane page = new StackPane();
        categories.getSelectionModel().selectedItemProperty().addListener((observable, was, chosen) -> {
            if (chosen != null) {
                page.getChildren().setAll(PLUGINS.equals(chosen) ? plugins() : appearance());
            }
        });
        categories.getSelectionModel().selectFirst();
        HBox.setHgrow(page, Priority.ALWAYS);
        HBox content = new HBox(16, categories, page);
        content.setPrefSize(WIDTH, HEIGHT);
        return content;
    }

    /** One choice per theme; choosing one applies it. */
    private Node appearance() {
        ToggleGroup choice = new ToggleGroup();
        List<Node> options = new ArrayList<>();
        for (FxTheme theme : FxTheme.values()) {
            RadioButton option = new RadioButton(theme.displayName());
            option.setToggleGroup(choice);
            option.setSelected(theme == themes.current());
            option.setOnAction(event -> themes.apply(theme));
            options.add(option);
        }
        return page("Theme", null, options);
    }

    /** One switch per tab of the last connection. */
    private Node plugins() {
        if (tabs.isEmpty()) {
            return page("Tabs", NO_TABS_YET, List.of());
        }
        List<Node> tabSwitches = new ArrayList<>();
        for (TabChoice tab : tabs) {
            CheckBox on = new CheckBox(tab.title());
            on.setSelected(switches.isOn(tab.id()));
            on.setOnAction(event -> switches.set(tab.id(), on.isSelected()));
            tabSwitches.add(on);
        }
        return page("Tabs", PLUGINS_NOTE, tabSwitches);
    }

    /** A page: its title, an optional note under it, then its controls, scrolling when they do not fit. */
    private static Node page(String title, String note, List<Node> controls) {
        Label heading = new Label(title);
        heading.getStyleClass().add(Styles.TEXT_BOLD);
        VBox page = new VBox(8, heading);
        if (note != null) {
            Label explanation = new Label(note);
            explanation.setWrapText(true);
            explanation.getStyleClass().add(Styles.TEXT_MUTED);
            page.getChildren().add(explanation);
        }
        page.getChildren().addAll(controls);
        page.setPadding(new Insets(4, 0, 0, 0));
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        return scroll;
    }
}
