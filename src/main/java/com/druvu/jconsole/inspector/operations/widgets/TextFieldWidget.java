package com.druvu.jconsole.inspector.operations.widgets;

import com.druvu.jconsole.inspector.operations.XOperations;
import com.druvu.jconsole.inspector.table.XTextField;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 * Default single-line text field. Wraps the existing {@link XTextField} so the
 * legacy drag-and-drop and Enter-to-invoke behavior is preserved unchanged.
 */
public final class TextFieldWidget implements ParamWidget {

    private final XTextField field;

    public TextFieldWidget(
            String defaultText,
            Class<?> expectedClass,
            int colWidth,
            boolean isCallable,
            JButton invokeButton,
            XOperations operation,
            String preset,
            String tooltip) {
        String initialText = (preset != null && !preset.isEmpty()) ? preset : defaultText;
        this.field = new XTextField(initialText, expectedClass, colWidth, isCallable, invokeButton, operation);
        this.field.setHorizontalAlignment(SwingConstants.CENTER);
        if (tooltip != null && !tooltip.isEmpty()) {
            this.field.setToolTipText(tooltip);
        }
    }

    @Override
    public JComponent component() {
        return field;
    }

    @Override
    public Object getValue() {
        return field.getValue();
    }
}
