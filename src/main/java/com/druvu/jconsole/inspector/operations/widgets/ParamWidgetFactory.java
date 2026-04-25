package com.druvu.jconsole.inspector.operations.widgets;

import com.druvu.jconsole.inspector.Utils;
import com.druvu.jconsole.inspector.operations.ParameterHint;
import com.druvu.jconsole.inspector.operations.XOperations;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.management.MBeanParameterInfo;
import javax.swing.JButton;
import lombok.extern.slf4j.Slf4j;

/**
 * Selects which {@link ParamWidget} to render for one operation parameter. Resolution order:
 *
 * <ol>
 *   <li>Markup hint in the parameter description ({@code {{combo:…}}}, {@code {{date:…}}}, etc.).
 *   <li>Parameter type ({@code boolean} / {@code Boolean} → checkbox).
 *   <li>Default: single-line {@link TextFieldWidget}.
 * </ol>
 */
@Slf4j
public final class ParamWidgetFactory {

    private ParamWidgetFactory() {}

    public static ParamWidget create(
            MBeanParameterInfo paramInfo,
            boolean isCallable,
            JButton invokeButton,
            XOperations operation,
            String preset) {

        Optional<ParameterHint> hint = ParameterHint.parse(paramInfo.getDescription());
        String tooltip = hint.map(ParameterHint::prose).orElse(paramInfo.getDescription());
        Class<?> type = resolveClass(paramInfo.getType());

        if (hint.isPresent()) {
            ParamWidget w =
                    createFromHint(hint.get(), paramInfo, type, tooltip, isCallable, invokeButton, operation, preset);
            if (w != null) {
                return w;
            }
        }

        if (isBoolean(type)) {
            return new BooleanWidget(tooltip, preset);
        }

        return defaultTextField(paramInfo, type, isCallable, invokeButton, operation, preset, tooltip);
    }

    private static ParamWidget createFromHint(
            ParameterHint hint,
            MBeanParameterInfo paramInfo,
            Class<?> type,
            String tooltip,
            boolean isCallable,
            JButton invokeButton,
            XOperations operation,
            String preset) {
        return switch (hint.tag()) {
            case "combo" -> {
                List<String> values = hint.optionsAsList();
                if (values.isEmpty()) {
                    log.warn(
                            "Empty {{combo}} options for parameter '{}' — falling back to text field",
                            paramInfo.getName());
                    yield null;
                }
                yield new ComboWidget(values, tooltip, preset);
            }
            case "date" -> new DateWidget(hint.options(), tooltip, preset);
            case "text" -> {
                Map<String, String> opts = hint.optionsAsKeyValue();
                int rows = parseIntOr(opts.get("rows"), MultilineWidget.DEFAULT_ROWS);
                int cols = parseIntOr(opts.get("cols"), MultilineWidget.DEFAULT_COLS);
                yield new MultilineWidget(rows, cols, tooltip, preset);
            }
            case "file" -> new FileWidget(hint.optionsAsList(), type, tooltip);
            default -> null; // unknown tag → fall through to type/default rules
        };
    }

    private static ParamWidget defaultTextField(
            MBeanParameterInfo paramInfo,
            Class<?> type,
            boolean isCallable,
            JButton invokeButton,
            XOperations operation,
            String preset,
            String tooltip) {
        return new TextFieldWidget("", type, 10, isCallable, invokeButton, operation, preset, tooltip);
    }

    private static boolean isBoolean(Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    private static Class<?> resolveClass(String name) {
        try {
            return Utils.getClass(name);
        } catch (ClassNotFoundException ex) {
            log.warn("Unknown parameter type '{}' — treating as Object for widget selection", name);
            return Object.class;
        }
    }

    private static int parseIntOr(String s, int fallback) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
