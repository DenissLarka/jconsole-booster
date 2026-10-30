package com.druvu.jconsole.fx.tab.mbeans.info;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.mbeans.common.Sections;
import com.druvu.jconsole.fx.tab.mbeans.common.TableCopy;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

/**
 * Two read-only Name/Value tables, one above the other: the feature's info and its descriptor. Header rows are bold.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class InfoView {

    private final SplitPane pane;

    public InfoView(InfoTables tables) {
        pane = new SplitPane(
                Sections.titled(tables.title(), table(tables.info())),
                Sections.titled(Messages.DESCRIPTOR, table(tables.descriptor())));
        pane.setOrientation(Orientation.VERTICAL);
    }

    public Node node() {
        return pane;
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
        table.setRowFactory(view -> new HeaderAwareRow());
        table.getItems().setAll(rows);
        TableCopy.install(table);
        return table;
    }

    /** Puts header rows in bold. */
    private static final class HeaderAwareRow extends TableRow<InfoRow> {
        @Override
        protected void updateItem(InfoRow row, boolean empty) {
            super.updateItem(row, empty);
            setStyle(!empty && row != null && row.header() ? "-fx-font-weight: bold;" : "");
        }
    }
}
