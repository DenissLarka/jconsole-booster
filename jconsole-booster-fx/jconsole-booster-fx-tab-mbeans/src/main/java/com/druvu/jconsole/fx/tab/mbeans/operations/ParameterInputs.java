package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.core.mbeans.MBeanValues;
import com.druvu.jconsole.fx.tab.TabHost;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javax.management.MBeanParameterInfo;

/**
 * Builds the input of an operation parameter, as {@link ParameterKind} says, filled with the remembered value.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ParameterInputs {

    private ParameterInputs() {}

    /**
     * @param preset the remembered value, empty for none
     * @param invoke invokes the operation, on Enter in a text field
     */
    static ParameterInput create(MBeanParameterInfo parameter, String preset, Runnable invoke, TabHost host) {
        ParameterKind kind = ParameterKind.of(parameter);
        ParameterInput input =
                switch (kind) {
                    case ParameterKind.SingleLine _ -> singleLine(parameter.getType(), preset, invoke);
                    case ParameterKind.Choice choice -> choice(choice, preset);
                    case ParameterKind.DateText date -> DateInput.create(date.format(), preset);
                    case ParameterKind.MultiLine area -> multiLine(area, preset);
                    case ParameterKind.FileUpload file -> new FileInput(file.patterns(), parameter.getType(), host);
                    case ParameterKind.Flag _ -> flag(preset);
                };
        String tooltip = tooltip(parameter, kind);
        if (!tooltip.isEmpty()) {
            Tooltip.install(input.node(), new Tooltip(tooltip));
        }
        return input;
    }

    private static ParameterInput singleLine(String type, String preset, Runnable invoke) {
        TextField field = new TextField(preset);
        field.setPromptText(simpleName(type));
        field.setPrefColumnCount(10);
        field.setOnAction(event -> invoke.run());
        return new ControlInput(field, field, field::getText);
    }

    private static ParameterInput choice(ParameterKind.Choice choice, String preset) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().setAll(choice.values());
        combo.setEditable(true);
        if (!preset.isEmpty()) {
            combo.setValue(preset);
        }
        return new ControlInput(
                combo, combo.getEditor(), () -> combo.getEditor().getText());
    }

    private static ParameterInput multiLine(ParameterKind.MultiLine area, String preset) {
        TextArea text = new TextArea(preset);
        text.setPrefRowCount(area.rows());
        text.setPrefColumnCount(area.columns());
        return new ControlInput(text, text, text::getText);
    }

    private static ParameterInput flag(String preset) {
        CheckBox check = new CheckBox();
        check.setSelected(Boolean.parseBoolean(preset));
        return new ControlInput(check, check, () -> Boolean.toString(check.isSelected()));
    }

    private static String tooltip(MBeanParameterInfo parameter, ParameterKind kind) {
        String prose = ParameterKind.tooltip(parameter);
        if (kind instanceof ParameterKind.DateText date && !date.format().isBlank()) {
            return prose.isEmpty() ? "format: " + date.format() : prose + " (format: " + date.format() + ")";
        }
        return prose;
    }

    /** {@code java.lang.String} reads {@code String}, {@code long} stays {@code long}. */
    static String simpleName(String type) {
        if (type == null) {
            return "";
        }
        String readable = MBeanValues.getReadableClassName(type);
        return readable.substring(readable.lastIndexOf('.') + 1);
    }
}
