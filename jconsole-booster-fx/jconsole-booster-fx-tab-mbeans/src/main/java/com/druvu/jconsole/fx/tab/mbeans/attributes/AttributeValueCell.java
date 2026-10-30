package com.druvu.jconsole.fx.tab.mbeans.attributes;

import com.druvu.jconsole.core.text.Messages;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;

/**
 * The value cell of the attribute table. A writable value shows in the accent color and edits in place (Enter sets it,
 * Escape cancels); a structured value shows in bold and opens in the viewer on a double click.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class AttributeValueCell extends TableCell<AttributeValue, String> {

    private static final String EDITABLE_STYLE = "-fx-text-fill: -color-accent-fg;";
    private static final String VIEWABLE_STYLE = "-fx-font-weight: bold;";

    private final BiConsumer<AttributeValue, String> commit;
    private final Tooltip tooltip = new Tooltip();

    /**
     * @param commit sets the attribute to the typed text
     * @param view opens a structured value in the viewer
     */
    AttributeValueCell(BiConsumer<AttributeValue, String> commit, Consumer<AttributeValue> view) {
        this.commit = commit;
        setOnMouseClicked(event -> {
            AttributeValue row = row();
            if (event.getClickCount() == 2 && !isEditing() && row != null && row.isViewable()) {
                view.accept(row);
            }
        });
    }

    @Override
    protected void updateItem(String text, boolean empty) {
        super.updateItem(text, empty);
        setGraphic(null);
        AttributeValue row = row();
        if (empty || row == null) {
            setText(null);
            setTooltip(null);
            setStyle("");
            setEditable(false);
            return;
        }
        setEditable(row.isEditable());
        setText(row.text());
        setStyle(style(row));
        tooltip.setText(tooltipText(row));
        setTooltip(tooltip.getText().isEmpty() ? null : tooltip);
    }

    @Override
    public void startEdit() {
        AttributeValue row = row();
        if (row == null || !row.isEditable()) {
            return;
        }
        super.startEdit();
        if (!isEditing()) {
            return;
        }
        TextField field = new TextField(row.value() == null ? "" : row.value().toString());
        field.setOnAction(event -> {
            commit.accept(row, field.getText());
            cancelEdit();
        });
        field.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
                event.consume();
            }
        });
        setText(null);
        setGraphic(field);
        field.requestFocus();
        field.selectAll();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
        setText(getItem());
    }

    /** The row this cell shows; the cell's own item is the row's text, which is what a copy takes. */
    private AttributeValue row() {
        int index = getIndex();
        if (getTableView() == null
                || index < 0
                || index >= getTableView().getItems().size()) {
            return null;
        }
        return getTableView().getItems().get(index);
    }

    private static String style(AttributeValue row) {
        if (row.isEditable()) {
            return EDITABLE_STYLE;
        }
        return row.isViewable() ? VIEWABLE_STYLE : "";
    }

    private static String tooltipText(AttributeValue row) {
        if (!row.isAvailable()) {
            return row.failure();
        }
        if (row.isViewable()) {
            return Messages.DOUBLE_CLICK_TO_VISUALIZE + ". " + row.text();
        }
        return row.text();
    }
}
