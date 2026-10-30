package com.druvu.jconsole.inspector.operations.widgets;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

/**
 * Checkbox used automatically for {@code boolean}/{@code Boolean} parameters. No markup tag is needed — the factory
 * selects this when the parameter type is boolean.
 */
public final class BooleanWidget implements ParamWidget {

    private final JCheckBox check;

    public BooleanWidget(String tooltip, String preset) {
        this.check = new JCheckBox();
        this.check.setSelected(Boolean.parseBoolean(preset));
        if (tooltip != null && !tooltip.isEmpty()) {
            check.setToolTipText(tooltip);
        }
    }

    @Override
    public JComponent component() {
        return check;
    }

    @Override
    public Object getValue() {
        return Boolean.toString(check.isSelected());
    }
}
