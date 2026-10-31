package com.druvu.jconsole.fx.tab.mbeans.info;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.DenseRows;
import com.druvu.jconsole.fx.tab.mbeans.common.Sections;
import com.druvu.jconsole.fx.tab.mbeans.common.TableCopy;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

/**
 * The feature's info as one read-only Name/Value table under its title: the properties block by block, then the
 * descriptor section. Header rows are bold; the section row is shaded too, so it stands apart from the blocks.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class InfoView {

    private static final String HEADER_STYLE = "-fx-font-weight: bold;";
    /** The table's own row background (a bottom line over the cell colour), with the subtle colour instead. */
    private static final String SECTION_STYLE = HEADER_STYLE
            + " -fx-background-color: -color-cell-border, -color-bg-subtle; -fx-background-insets: 0, 0 0 1 0;";

    private final Node node;

    public InfoView(InfoTable info) {
        node = Sections.titled(info.title(), table(info.rows()));
    }

    public Node node() {
        return node;
    }

    private static TableView<InfoRow> table(List<InfoRow> rows) {
        TableView<InfoRow> table = new TableView<>();
        TableColumn<InfoRow, String> name = new TableColumn<>(Messages.NAME);
        name.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        name.setPrefWidth(160);
        name.setSortable(false);
        TableColumn<InfoRow, String> value = new TableColumn<>(Messages.VALUE);
        value.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        value.setSortable(false);
        table.getColumns().setAll(List.of(name, value));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        DenseRows.apply(table);
        table.setRowFactory(view -> new KindAwareRow());
        table.getItems().setAll(rows);
        TableCopy.install(table);
        return table;
    }

    private static String style(InfoRow row) {
        return switch (row.kind()) {
            case SECTION -> SECTION_STYLE;
            case HEADER -> HEADER_STYLE;
            case PROPERTY -> "";
        };
    }

    /** Styles each row by its kind. */
    private static final class KindAwareRow extends TableRow<InfoRow> {
        @Override
        protected void updateItem(InfoRow row, boolean empty) {
            super.updateItem(row, empty);
            setStyle(empty || row == null ? "" : style(row));
        }
    }
}
