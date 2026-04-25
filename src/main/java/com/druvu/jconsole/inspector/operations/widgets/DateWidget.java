package com.druvu.jconsole.inspector.operations.widgets;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Date input for {@code {{date:format}}}. v1 implementation uses
 * {@link JFormattedTextField} backed by a {@link SimpleDateFormat} so the
 * field validates the user's input against the operator-supplied pattern but
 * does not render a popup calendar (zero new dependencies).
 *
 * <p>If the format string is malformed the widget falls back to a plain text
 * field with the format echoed in the tooltip.
 */
@Slf4j
public final class DateWidget implements ParamWidget {

    private final JFormattedTextField field;
    private final boolean valid;

    public DateWidget(String dateFormat, String tooltip, String preset) {
        SimpleDateFormat sdf = null;
        try {
            sdf = new SimpleDateFormat(dateFormat);
            sdf.setLenient(false);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid date format '{}' — falling back to plain text input", dateFormat);
        }
        this.valid = (sdf != null);
        if (sdf != null) {
            DateFormatter formatter = new DateFormatter(sdf);
            formatter.setOverwriteMode(false);
            this.field = new JFormattedTextField(new DefaultFormatterFactory(formatter));
            Date initial = new Date();
            if (preset != null && !preset.isEmpty()) {
                try {
                    initial = sdf.parse(preset);
                } catch (java.text.ParseException ex) {
                    log.warn("Could not parse preset date '{}' for format '{}'", preset, dateFormat);
                }
            }
            this.field.setValue(initial);
            this.field.setColumns(Math.max(10, dateFormat.length()));
        } else {
            this.field = new JFormattedTextField();
            if (preset != null && !preset.isEmpty()) {
                this.field.setText(preset);
            }
            this.field.setColumns(Math.max(10, dateFormat == null ? 10 : dateFormat.length()));
        }

        StringBuilder tip = new StringBuilder();
        if (tooltip != null && !tooltip.isEmpty()) {
            tip.append(tooltip);
        }
        if (dateFormat != null && !dateFormat.isEmpty()) {
            if (!tip.isEmpty()) {
                tip.append(" — ");
            }
            tip.append("format: ").append(dateFormat);
        }
        if (!tip.isEmpty()) {
            field.setToolTipText(tip.toString());
        }
    }

    @Override
    public JComponent component() {
        return field;
    }

    @Override
    public Object getValue() {
        if (!valid) {
            return field.getText();
        }
        try {
            field.commitEdit();
        } catch (java.text.ParseException ignored) {
            return field.getText();
        }
        Object v = field.getValue();
        if (v instanceof Date d) {
            return ((SimpleDateFormat) ((DateFormatter) field.getFormatter()).getFormat()).format(d);
        }
        return field.getText();
    }
}
