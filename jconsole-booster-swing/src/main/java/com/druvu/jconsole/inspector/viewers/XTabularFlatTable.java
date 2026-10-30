package com.druvu.jconsole.inspector.viewers;

import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Flat multi-column view of a {@link TabularData}: one row per composite, one column per {@code CompositeType} field.
 * Replaces the JDK default per-row arrow navigator (one composite at a time, prev/next buttons), which becomes unusable
 * past a handful of rows.
 *
 * <p>Column ordering puts {@link TabularType#getIndexNames() index columns} first (in declared order), followed by
 * remaining fields alphabetically. Index columns render in italic so the row-key tuple is visually obvious.
 *
 * <p>Rows are sorted by the index-name tuple via {@link XOpenTypeViewer.TabularDataComparator}.
 *
 * <p>v1 limitation: nested complex cell values (CompositeData / TabularData inside a row) render via {@code toString()}
 * — no double-click drill-down.
 */
final class XTabularFlatTable extends XOpenTypeViewer.XOpenTypeData {

    private final TabularType type;
    private final String[] columnNames;
    private final Set<String> indexNameSet;
    private Font normalFont;
    private Font italicFont;

    @SuppressWarnings("unchecked")
    XTabularFlatTable(XOpenTypeViewer.XOpenTypeData parent, TabularData tabular) {
        super(parent);
        this.type = tabular.getTabularType();
        CompositeType rowType = type.getRowType();
        List<String> indexNames = type.getIndexNames();
        this.indexNameSet = new HashSet<>(indexNames);

        List<String> ordered = new ArrayList<>(indexNames);
        List<String> rest = new ArrayList<>(rowType.keySet());
        rest.removeAll(indexNames);
        Collections.sort(rest);
        ordered.addAll(rest);
        this.columnNames = ordered.toArray(new String[0]);

        initTable(this.columnNames);
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(true);
        setCellSelectionEnabled(true);

        List<CompositeData> rows = new ArrayList<>((Collection<CompositeData>) tabular.values());
        Collections.sort(rows, new XOpenTypeViewer.TabularDataComparator(type));
        DefaultTableModel model = (DefaultTableModel) getModel();
        for (CompositeData row : rows) {
            Object[] data = new Object[columnNames.length];
            for (int i = 0; i < columnNames.length; i++) {
                data[i] = row.get(columnNames[i]);
            }
            model.addRow(data);
        }
        model.newDataAvailable(new TableModelEvent(model));
    }

    @Override
    public void viewed(XOpenTypeViewer viewer) {
        viewer.setOpenType(this);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Object value = getValueAt(row, column);
        Component comp =
                renderer.getTableCellRendererComponent(this, value, isCellSelected(row, column), false, row, column);
        if (normalFont == null) {
            normalFont = comp.getFont();
            italicFont = normalFont.deriveFont(Font.ITALIC);
        }
        comp.setFont(indexNameSet.contains(columnNames[column]) ? italicFont : normalFont);
        return comp;
    }

    @Override
    public String getToolTip(int row, int col) {
        Object value = getModel().getValueAt(row, col);
        return value == null ? null : value.toString();
    }

    @Override
    public String toString() {
        return type == null ? "" : type.getDescription();
    }

    String[] columnNames() {
        return columnNames;
    }

    String typeName() {
        return type == null ? "tabular" : type.getTypeName();
    }
}
