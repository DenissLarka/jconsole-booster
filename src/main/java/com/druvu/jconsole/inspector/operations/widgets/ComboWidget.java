package com.druvu.jconsole.inspector.operations.widgets;

import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;

/**
 * Editable dropdown picker for {@code {{combo:A,B,C}}}. The listed values act
 * as suggestions; the user can also type any other value into the editor.
 */
public final class ComboWidget implements ParamWidget {

    private final JComboBox<String> combo;

    public ComboWidget(List<String> values, String tooltip, String preset) {
        this.combo = new JComboBox<>(values.toArray(new String[0]));
        this.combo.setEditable(true);
        if (tooltip != null && !tooltip.isEmpty()) {
            combo.setToolTipText(tooltip);
        }
        if (preset != null && !preset.isEmpty()) {
            combo.setSelectedItem(preset);
        }
    }

    @Override
    public JComponent component() {
        return combo;
    }

    @Override
    public Object getValue() {
        Object editorValue = combo.getEditor().getItem();
        if (editorValue != null && !editorValue.toString().isEmpty()) {
            return editorValue.toString();
        }
        Object selected = combo.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }
}
