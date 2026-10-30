package com.druvu.jconsole.fx.tab.summary;

import atlantafx.base.theme.Styles;
import com.druvu.jconsole.core.text.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * The VM Summary page: the short values in titled groups that wrap with the window's width, then the long ones (VM
 * arguments and paths) in read-only text areas, where they can be selected and copied.
 *
 * <p>The page is built once and then only its texts change, so a selection in a text area survives the next update. It
 * is built again only when the rows themselves change. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class SummaryView {

    static final String VALUE_STYLE = "summary-value";
    static final String PATH_STYLE = "summary-path";

    private static final String NO_PLATFORM_MXBEANS =
            "The target JVM does not expose the platform MXBeans this page reads.";
    private static final int MAX_PATH_LINES = 8;

    private final Label readAt = new Label();
    private final Label unavailable = new Label(NO_PLATFORM_MXBEANS);
    private final FlowPane sectionsPane = new FlowPane(32, 16);
    private final VBox pathsPane = new VBox(8);
    private final VBox page;
    private final ScrollPane root;

    // the value nodes of the page as built, in the order of the summary's rows
    private final List<Label> values = new ArrayList<>();
    private final List<TextArea> pathValues = new ArrayList<>();
    private List<String> builtLayout = List.of();

    SummaryView() {
        Label heading = new Label(Messages.SUMMARY_TAB_TAB_NAME);
        heading.getStyleClass().add(Styles.TITLE_3);
        readAt.getStyleClass().add(Styles.TEXT_MUTED);
        unavailable.setWrapText(true);
        setShown(unavailable, false);

        page = new VBox(16, new VBox(2, heading, readAt), unavailable, sectionsPane, pathsPane);
        page.setPadding(new Insets(16));
        root = new ScrollPane(page);
        root.setFitToWidth(true);
    }

    Node node() {
        return root;
    }

    /** The page inside the scroll pane, for tests to look nodes up in. */
    Parent page() {
        return page;
    }

    /** Puts a fresh reading on the page. */
    void show(VmSummary summary) {
        Objects.requireNonNull(summary, "summary");
        List<String> layout = layoutOf(summary);
        if (!layout.equals(builtLayout)) {
            build(summary);
            builtLayout = layout;
        }
        readAt.setText("Read at " + summary.readAt());
        int index = 0;
        for (SummarySection section : summary.sections()) {
            for (SummaryRow row : section.rows()) {
                setIfChanged(values.get(index++), row.value());
            }
        }
        for (int i = 0; i < summary.paths().size(); i++) {
            setIfChanged(pathValues.get(i), summary.paths().get(i).value());
        }
        setShown(unavailable, false);
    }

    /** Says that the target has nothing this page can read. */
    void showUnavailable() {
        sectionsPane.getChildren().clear();
        pathsPane.getChildren().clear();
        values.clear();
        pathValues.clear();
        builtLayout = List.of();
        readAt.setText("");
        setShown(unavailable, true);
    }

    private void build(VmSummary summary) {
        sectionsPane.getChildren().clear();
        pathsPane.getChildren().clear();
        values.clear();
        pathValues.clear();
        for (SummarySection section : summary.sections()) {
            sectionsPane.getChildren().add(sectionNode(section));
        }
        for (SummaryRow path : summary.paths()) {
            Label label = new Label(path.label());
            label.getStyleClass().add(Styles.TEXT_BOLD);
            TextArea value = new TextArea();
            value.getStyleClass().add(PATH_STYLE);
            value.setEditable(false);
            value.setWrapText(true);
            value.setPrefRowCount(Math.clamp(path.value().lines().count(), 1, MAX_PATH_LINES));
            pathValues.add(value);
            pathsPane.getChildren().add(new VBox(4, label, value));
        }
    }

    private Node sectionNode(SummarySection section) {
        Label title = new Label(section.title());
        title.getStyleClass().add(Styles.TITLE_4);
        GridPane rows = new GridPane(12, 4);
        int rowIndex = 0;
        for (SummaryRow row : section.rows()) {
            Label label = new Label(row.label());
            label.getStyleClass().add(Styles.TEXT_MUTED);
            Label value = valueLabel();
            values.add(value);
            rows.addRow(rowIndex++, label, value);
        }
        return new VBox(6, title, rows);
    }

    /** A value with a Copy menu: a label cannot be selected, and a value is often wanted elsewhere. */
    private static Label valueLabel() {
        Label value = new Label();
        value.getStyleClass().add(VALUE_STYLE);
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(value.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        value.setContextMenu(new ContextMenu(copy));
        return value;
    }

    /** What decides the page's shape: the groups and the labels of their rows. */
    private static List<String> layoutOf(VmSummary summary) {
        List<String> layout = new ArrayList<>();
        for (SummarySection section : summary.sections()) {
            layout.add(section.title());
            section.rows().forEach(row -> layout.add(section.title() + "/" + row.label()));
        }
        summary.paths().forEach(row -> layout.add("paths/" + row.label()));
        return layout;
    }

    // Setting the same text again would drop the user's selection in a text area.
    private static void setIfChanged(Label label, String text) {
        if (!text.equals(label.getText())) {
            label.setText(text);
        }
    }

    private static void setIfChanged(TextArea area, String text) {
        if (!text.equals(area.getText())) {
            area.setText(text);
        }
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
