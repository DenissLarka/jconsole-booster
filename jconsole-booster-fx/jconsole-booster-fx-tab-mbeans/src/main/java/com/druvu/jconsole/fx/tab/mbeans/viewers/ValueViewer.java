package com.druvu.jconsole.fx.tab.mbeans.viewers;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.TabHost;
import com.druvu.jconsole.fx.tab.mbeans.common.TableCopy;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

/**
 * Shows a structured value one level at a time: double click a bold cell to open it, {@code <<} to go back, {@code <}
 * and {@code >} to walk an array of composites. A tabular level can be copied or saved as CSV.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class ValueViewer {

    private final TabHost host;
    /** The opened levels, the shown one on top. */
    private final Deque<ValueLevel> opened = new ArrayDeque<>();

    private final TableView<List<Object>> table = new TableView<>();
    private final Label caption = new Label();
    private final Button back = new Button(Messages.A_LOT_LESS_THAN);
    private final Button previous = new Button(Messages.LESS_THAN);
    private final Label position = new Label();
    private final Button next = new Button(Messages.GREATER_THAN);
    private final Button copyCsv = new Button("Copy as CSV");
    private final Button saveCsv = new Button("Save as CSV…");
    private final BorderPane pane = new BorderPane(table);

    public ValueViewer(ValueLevel level, TabHost host) {
        this.host = host;
        back.setOnAction(event -> goBack());
        previous.setOnAction(event -> walk(false));
        next.setOnAction(event -> walk(true));
        copyCsv.setTooltip(new Tooltip("Copy the entire table to the clipboard as CSV"));
        copyCsv.setOnAction(event -> copyCsv());
        saveCsv.setTooltip(new Tooltip("Save the entire table to a .csv file"));
        saveCsv.setOnAction(event -> saveCsv());
        HBox bar = new HBox(6, back, previous, position, next, caption, copyCsv, saveCsv);
        bar.setPadding(new Insets(0, 0, 6, 0));
        pane.setTop(bar);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableCopy.install(table);
        opened.push(level);
        render();
    }

    public Node node() {
        return pane;
    }

    /** The level on screen, for tests. */
    ValueLevel shown() {
        return opened.getFirst();
    }

    void open(ValueLevel level) {
        opened.push(level);
        render();
    }

    private void goBack() {
        if (opened.size() > 1) {
            opened.pop();
            render();
        }
    }

    private void walk(boolean forward) {
        if (opened.getFirst() instanceof CompositeArrayLevel array) {
            opened.pop();
            opened.push(forward ? array.next() : array.previous());
            render();
        }
    }

    private void render() {
        ValueLevel level = opened.getFirst();
        caption.setText(level.title());
        List<String> names = level.columns();
        table.getColumns().clear();
        for (int i = 0; i < names.size(); i++) {
            table.getColumns().add(column(names.get(i), i));
        }
        table.getItems().setAll(level.rows());
        back.setDisable(opened.size() < 2);
        boolean walking = level instanceof CompositeArrayLevel;
        previous.setVisible(walking);
        next.setVisible(walking);
        position.setVisible(walking);
        if (level instanceof CompositeArrayLevel array) {
            previous.setDisable(!array.hasPrevious());
            next.setDisable(!array.hasNext());
            position.setText(MessageFormat.format(
                    Messages.MBEANS_TAB_COMPOSITE_NAVIGATION_MULTIPLE,
                    array.index() + 1,
                    array.elements().size()));
        }
        boolean tabular = level instanceof TabularLevel;
        copyCsv.setVisible(tabular);
        saveCsv.setVisible(tabular);
    }

    private TableColumn<List<Object>, Object> column(String name, int index) {
        TableColumn<List<Object>, Object> column = new TableColumn<>(name);
        column.setCellValueFactory(
                cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get(index)));
        column.setCellFactory(view -> new PartCell(this::open));
        column.setSortable(false);
        return column;
    }

    private void copyCsv() {
        if (opened.getFirst() instanceof TabularLevel tabular) {
            ClipboardContent content = new ClipboardContent();
            content.putString(tabular.csv());
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private void saveCsv() {
        if (!(opened.getFirst() instanceof TabularLevel tabular)) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(tabular.title() + ".csv");
        File target = chooser.showSaveDialog(
                pane.getScene() == null ? null : pane.getScene().getWindow());
        if (target == null) {
            return;
        }
        try {
            Files.writeString(target.toPath(), tabular.csv(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            host.showError("Could not save " + target, e);
        }
    }

    /** A plain value as text; a part that opens in bold, opened by a double click. */
    private static final class PartCell extends TableCell<List<Object>, Object> {

        private final Tooltip tooltip = new Tooltip();

        PartCell(Consumer<ValueLevel> open) {
            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && getItem() instanceof ValueLevel level) {
                    open.accept(level);
                }
            });
        }

        @Override
        protected void updateItem(Object part, boolean empty) {
            super.updateItem(part, empty);
            if (empty) {
                setText(null);
                setTooltip(null);
                setStyle("");
                return;
            }
            boolean opens = part instanceof ValueLevel;
            String text = part instanceof ValueLevel level ? level.title() : ValueText.plain(part);
            setText(text);
            setStyle(opens ? "-fx-font-weight: bold;" : "");
            tooltip.setText(opens ? Messages.DOUBLE_CLICK_TO_VISUALIZE + ". " + text : text);
            setTooltip(text.isEmpty() ? null : tooltip);
        }
    }
}
