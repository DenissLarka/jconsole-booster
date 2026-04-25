package com.druvu.jconsole.inspector.operations.widgets;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Multi-line text area for {@code {{text:rows=N,cols=M}}}. */
public final class MultilineWidget implements ParamWidget {

    public static final int DEFAULT_ROWS = 8;
    public static final int DEFAULT_COLS = 60;

    private final JTextArea area;
    private final JScrollPane scroll;

    public MultilineWidget(int rows, int cols, String tooltip, String preset) {
        this.area = new JTextArea(rows, cols);
        area.setLineWrap(false);
        if (preset != null && !preset.isEmpty()) {
            area.setText(preset);
        }
        if (tooltip != null && !tooltip.isEmpty()) {
            area.setToolTipText(tooltip);
        }
        this.scroll = new JScrollPane(area);
    }

    @Override
    public JComponent component() {
        return scroll;
    }

    @Override
    public Object getValue() {
        return area.getText();
    }
}
