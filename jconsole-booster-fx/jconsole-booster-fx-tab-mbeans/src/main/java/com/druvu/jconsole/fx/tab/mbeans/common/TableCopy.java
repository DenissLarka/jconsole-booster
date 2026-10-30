package com.druvu.jconsole.fx.tab.mbeans.common;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Lets the user copy what a read-only table shows: the selected rows, or every row when none is selected, one line per
 * row with the cells separated by tabs, from a context menu or the platform's copy shortcut.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class TableCopy {

    private static final KeyCombination COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

    private TableCopy() {}

    public static void install(TableView<?> table) {
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(event -> copy(table));
        MenuItem selectAll = new MenuItem("Select all");
        selectAll.setOnAction(event -> table.getSelectionModel().selectAll());
        table.setContextMenu(new ContextMenu(copy, selectAll));
        table.setOnKeyPressed(event -> {
            if (COPY.match(event)) {
                copy(table);
                event.consume();
            }
        });
    }

    /** Puts the rows on the clipboard. */
    public static void copy(TableView<?> table) {
        List<Integer> rows = table.getSelectionModel().getSelectedIndices().isEmpty()
                ? IntStream.range(0, table.getItems().size()).boxed().toList()
                : List.copyOf(table.getSelectionModel().getSelectedIndices());
        String text = rows.stream().map(row -> rowText(table, row)).collect(Collectors.joining("\n"));
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static String rowText(TableView<?> table, int row) {
        return table.getVisibleLeafColumns().stream()
                .map(column -> cellText(column, row))
                .collect(Collectors.joining("\t"));
    }

    private static String cellText(TableColumn<?, ?> column, int row) {
        Object value = column.getCellData(row);
        return value == null ? "" : value.toString();
    }
}
