package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.core.operations.ParameterHint;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.management.MBeanParameterInfo;

/**
 * Which input an operation parameter gets. The first {@code {{tag:options}}} in the parameter's description decides;
 * without a known tag, a boolean gets a check box and anything else a text field.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public sealed interface ParameterKind {

    int DEFAULT_ROWS = 8;
    int DEFAULT_COLUMNS = 60;

    /** A one-line text field; Enter invokes the operation. */
    record SingleLine() implements ParameterKind {}

    /** {@code {{combo:A,B}}}: an editable list of suggestions. */
    record Choice(List<String> values) implements ParameterKind {
        public Choice {
            values = List.copyOf(values);
        }
    }

    /** {@code {{date:dd.MM.yyyy}}}: a date picker writing that format. */
    record DateText(String format) implements ParameterKind {}

    /** {@code {{text:rows=N,cols=M}}}: a text area. */
    record MultiLine(int rows, int columns) implements ParameterKind {}

    /** {@code {{file:*.csv,*.json}}}: a file whose content is the value. */
    record FileUpload(List<String> patterns) implements ParameterKind {
        public FileUpload {
            patterns = List.copyOf(patterns);
        }
    }

    /** A boolean parameter: a check box. */
    record Flag() implements ParameterKind {}

    static ParameterKind of(MBeanParameterInfo parameter) {
        return ParameterHint.parse(parameter.getDescription())
                .flatMap(ParameterKind::fromHint)
                .orElseGet(() -> isBoolean(parameter.getType()) ? new Flag() : new SingleLine());
    }

    /** The parameter's description without its markup, for the tooltip; empty for none. */
    static String tooltip(MBeanParameterInfo parameter) {
        String description = parameter.getDescription() == null ? "" : parameter.getDescription();
        return ParameterHint.parse(description).map(ParameterHint::prose).orElse(description);
    }

    /** The input a tag asks for; empty for an unknown tag or a combo without values. */
    private static Optional<ParameterKind> fromHint(ParameterHint hint) {
        return switch (hint.tag()) {
            case "combo" ->
                hint.optionsAsList().isEmpty() ? Optional.empty() : Optional.of(new Choice(hint.optionsAsList()));
            case "date" -> Optional.of(new DateText(hint.options()));
            case "text" -> {
                Map<String, String> options = hint.optionsAsKeyValue();
                yield Optional.of(new MultiLine(
                        number(options.get("rows"), DEFAULT_ROWS), number(options.get("cols"), DEFAULT_COLUMNS)));
            }
            case "file" -> Optional.of(new FileUpload(hint.optionsAsList()));
            default -> Optional.empty();
        };
    }

    private static boolean isBoolean(String type) {
        return boolean.class.getName().equals(type) || Boolean.class.getName().equals(type);
    }

    private static int number(String text, int fallback) {
        try {
            return text == null ? fallback : Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
