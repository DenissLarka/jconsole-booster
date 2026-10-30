package com.druvu.jconsole.inspector.viewers;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.management.openmbean.TabularData;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.table.TableModel;
import lombok.extern.slf4j.Slf4j;

/**
 * Top-level viewer for {@link TabularData}. Replaces the JDK navigator chrome (per-row arrows, parent button) with a
 * clean flat-table view plus a small toolbar for clipboard / file CSV export.
 *
 * <p>Used only when the operation result is a top-level TabularData. Nested TabularData reached by drilling into a
 * composite cell still rides inside {@link XOpenTypeViewer} so the {@code <<} back button remains available.
 */
@Slf4j
final class TabularDataPanel extends JPanel {

    private final XTabularFlatTable table;

    TabularDataPanel(TabularData data) {
        super(new BorderLayout());
        this.table = new XTabularFlatTable(null, data);

        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        JButton copy = new JButton("Copy as CSV");
        copy.setToolTipText("Copy the entire table to the clipboard as CSV");
        copy.addActionListener(e -> copyAsCsv());
        JButton save = new JButton("Save as CSV…");
        save.setToolTipText("Save the entire table to a .csv file");
        save.addActionListener(e -> saveAsCsv());
        bar.add(copy);
        bar.add(save);

        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void copyAsCsv() {
        String csv = buildCsv();
        StringSelection sel = new StringSelection(csv);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
    }

    private void saveAsCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(table.typeName() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        try {
            Files.writeString(target, buildCsv(), StandardCharsets.UTF_8);
            log.info("Wrote {} rows to {}", table.getRowCount(), target);
        } catch (IOException ex) {
            log.warn("Could not write CSV to {}: {}", target, ex.getMessage());
            JOptionPane.showMessageDialog(
                    this, "Could not save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildCsv() {
        String[] cols = table.columnNames();
        TableModel model = table.getModel();
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < cols.length; c++) {
            if (c > 0) {
                sb.append(',');
            }
            sb.append(csvEscape(cols[c]));
        }
        sb.append("\r\n");
        for (int r = 0; r < model.getRowCount(); r++) {
            for (int c = 0; c < cols.length; c++) {
                if (c > 0) {
                    sb.append(',');
                }
                sb.append(csvEscape(model.getValueAt(r, c)));
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    private static String csvEscape(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        boolean quote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (quote) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
