package com.druvu.jconsole.fx.tab.threads;

import com.druvu.jconsole.fx.charts.TimeSeriesChart;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

/**
 * The Threads tab's content in a scene, where its controls get their skins and can be looked up; the controls a test
 * reads and drives. Use it on the FX thread.
 */
final class ThreadsScreen {

    private final Parent root;

    ThreadsScreen(Node content) {
        root = new StackPane(content);
        Scene scene = new Scene(root);
        scene.getRoot().applyCss();
        root.layout();
    }

    /** Lays the content out again, which gives the controls added since then their skins. */
    void layout() {
        root.applyCss();
        root.layout();
    }

    TimeSeriesChart chart() {
        return (TimeSeriesChart) find(root, ".time-series-chart");
    }

    TextField filter() {
        return (TextField) find(root, "." + ThreadsView.FILTER_STYLE_CLASS);
    }

    String count() {
        return ((Label) find(root, "." + ThreadsView.COUNT_STYLE_CLASS)).getText();
    }

    String message() {
        return ((Label) find(root, "." + ThreadsView.MESSAGE_STYLE_CLASS)).getText();
    }

    void detectDeadlock() {
        ((Button) find(root, "." + ThreadsView.DETECT_STYLE_CLASS)).fire();
    }

    List<String> tabTitles() {
        return tabs().getTabs().stream().map(Tab::getText).toList();
    }

    int selectedTab() {
        return tabs().getSelectionModel().getSelectedIndex();
    }

    /** The thread list of the tab at {@code tab}: 0 for all threads, then one per deadlock. */
    @SuppressWarnings("unchecked") // the thread lists list ThreadEntry
    ListView<ThreadEntry> list(int tab) {
        return (ListView<ThreadEntry>) find(tabContent(tab), ".list-view");
    }

    List<String> names(int tab) {
        return list(tab).getItems().stream().map(ThreadEntry::name).toList();
    }

    String details(int tab) {
        return ((TextArea) find(tabContent(tab), "." + ThreadBrowser.DETAILS_STYLE_CLASS)).getText();
    }

    private TabPane tabs() {
        return (TabPane) find(root, ".tab-pane");
    }

    private Node tabContent(int tab) {
        return tabs().getTabs().get(tab).getContent();
    }

    private static Node find(Node within, String selector) {
        Node found = within.lookup(selector);
        if (found == null) {
            throw new IllegalStateException("Nothing matches " + selector);
        }
        return found;
    }
}
