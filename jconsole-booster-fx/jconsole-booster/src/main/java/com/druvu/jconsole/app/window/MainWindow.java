package com.druvu.jconsole.app.window;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.TabHost;
import com.druvu.lib.fx.notify.Notifications;
import com.druvu.lib.fx.status.StatusBarModel;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * The app's one window: the menus, the optional colour band, the connection's tabs (or a hint while there are none) and
 * the status bar. It shows what it is told and asks the user when told to; the connection's life is decided elsewhere.
 * It is also the {@link TabHost} the tabs talk to. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class MainWindow implements TabHost {

    private static final String NOT_CONNECTED_HINT = "Not connected. Connection > New Connection… opens a JVM.";

    /** macOS lifts the menus out of the window, leaving the bar behind as an empty strip. */
    private static final boolean MAC =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");

    private static final double WIDTH = 1100;
    private static final double HEIGHT = 760;

    private final Stage stage;
    private final Notifications notifications;
    private final VBox top = new VBox();
    private final TabPane tabPane = new TabPane();
    private final Label notConnected = new Label(NOT_CONNECTED_HINT);
    private final StatusBar statusBar;

    /**
     * Builds the window's scene on {@code stage}; the caller shows it.
     *
     * @param stage the app's primary stage
     * @param tasks the background work, for the status bar's spinner
     */
    public MainWindow(Stage stage, StatusBarModel tasks) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.statusBar = new StatusBar(tasks);
        this.notifications = new Notifications(stage);

        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        notConnected.getStyleClass().add("not-connected-hint");
        statusBar.node().getStyleClass().add("status-bar");

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(new StackPane(tabPane, notConnected));
        root.setBottom(statusBar.node());
        root.getStylesheets()
                .add(Objects.requireNonNull(
                                MainWindow.class.getResource("jconsole-booster.css"), "jconsole-booster.css")
                        .toExternalForm());

        stage.setScene(new Scene(root, WIDTH, HEIGHT));
        stage.getIcons().setAll(AppIcon.images());
        stage.setOnHidden(event -> notifications.close());
        showTabs(List.of(), null);
        showNotConnected("Not connected");
    }

    /** Puts the menus at the top; on macOS they move to the screen's menu bar. */
    public void setMenuBar(MenuBar menuBar) {
        menuBar.setUseSystemMenuBar(true);
        if (MAC) {
            // The node must stay in the scene for macOS to adopt the menus; it is collapsed, never removed.
            menuBar.getStyleClass().add("system-menu-bar-host");
        }
        top.getChildren().addFirst(menuBar);
    }

    /** A thin band in the colour given at launch, under the menus. */
    public void showColorBand(Color color) {
        top.getChildren().add(ColorBand.band(color));
    }

    /**
     * Shows a connection's tabs in their order.
     *
     * @param tabs the tabs, in place order
     * @param selected the tab to show first, or {@code null} for the first one
     */
    public void showTabs(List<ConnectionTab> tabs, ConnectionTab selected) {
        tabPane.getTabs().clear();
        for (ConnectionTab connectionTab : tabs) {
            Tab tab = new Tab(connectionTab.title(), connectionTab.content());
            tab.setUserData(connectionTab);
            tabPane.getTabs().add(tab);
            if (connectionTab == selected) {
                tabPane.getSelectionModel().select(tab);
            }
        }
        boolean none = tabs.isEmpty();
        tabPane.setVisible(!none);
        notConnected.setVisible(none);
    }

    /** Removes the tabs: the connection is closed. */
    public void clearTabs() {
        showTabs(List.of(), null);
        statusBar.clearLastUpdate();
    }

    /** Greys out a tab that failed; its content stays as it was last shown. */
    public void disableTab(ConnectionTab failed) {
        tabPane.getTabs().stream().filter(tab -> tab.getUserData() == failed).forEach(tab -> tab.setDisable(true));
    }

    /** The title and status bar of a working connection. */
    public void showConnected(String connectionName, String state) {
        stage.setTitle(WindowTitle.connectedTo(connectionName));
        statusBar.showConnection(state);
    }

    /** The title and status bar without a working connection: none yet, one opening, or one lost. */
    public void showNotConnected(String state) {
        stage.setTitle(WindowTitle.notConnected());
        statusBar.showConnection(state);
    }

    public void showLastUpdate(LocalTime at) {
        statusBar.showLastUpdate(at);
    }

    /**
     * Asks a yes-or-no question and waits for the answer.
     *
     * @param confirmText the label of the button that says yes
     * @return whether the user said yes
     */
    public boolean confirm(String title, String message, String confirmText) {
        ButtonType yes = new ButtonType(confirmText, ButtonData.OK_DONE);
        Alert question = new Alert(Alert.AlertType.CONFIRMATION, message, yes, ButtonType.CANCEL);
        question.initOwner(stage);
        question.setTitle(title);
        question.setHeaderText(title);
        return question.showAndWait().filter(yes::equals).isPresent();
    }

    @Override
    public Window window() {
        return stage;
    }

    /** Shows the failure without waiting: it may arrive while the user is busy elsewhere. */
    @Override
    public void showError(String title, Throwable failure) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.initOwner(stage);
        error.setTitle(title);
        error.setHeaderText(title);
        error.setContentText(failure.getMessage() == null ? failure.toString() : failure.getMessage());
        TextArea trace = new TextArea(stackTrace(failure));
        trace.setEditable(false);
        error.getDialogPane().setExpandableContent(trace);
        error.show();
    }

    @Override
    public void notice(String message) {
        notifications.info(message);
    }

    private static String stackTrace(Throwable failure) {
        StringWriter text = new StringWriter();
        failure.printStackTrace(new PrintWriter(text));
        return text.toString();
    }
}
