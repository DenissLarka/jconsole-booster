package com.druvu.jconsole.fx.tab.mbeans.viewers;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

/**
 * One screen of the value viewer: a table of the parts of a structured value. A part that is structured itself is a
 * level too, which the user opens with a double click and leaves with the back button.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public sealed interface ValueLevel permits CompositeLevel, CompositeArrayLevel, TabularLevel, ValueListLevel {

    /** The value's caption, and what a cell holding it shows. */
    String title();

    /** Whether there is nothing to open. */
    boolean isEmpty();

    List<String> columns();

    /** The rows, one cell per column: a plain value, or a level the user can open. */
    List<List<Object>> rows();

    /**
     * The level that shows a value: composite and tabular data, arrays, collections and maps; empty for a value the
     * viewer does not open.
     */
    static Optional<ValueLevel> of(Object value) {
        return switch (value) {
            case null -> Optional.empty();
            case CompositeData composite -> Optional.of(new CompositeLevel(composite));
            case TabularData tabular -> Optional.of(TabularLevel.of(tabular));
            default -> sequence(value);
        };
    }

    /** What a table cell holds for a part of a value: a level to open, or the plain value; empty for {@code null}. */
    static Object cell(Object value) {
        if (value == null) {
            return "";
        }
        return of(value)
                .filter(level -> !level.isEmpty())
                .<Object>map(level -> level)
                .orElse(value);
    }

    /** An array of composites, navigated one element at a time; any other sequence as a list of values. */
    private static Optional<ValueLevel> sequence(Object value) {
        return elements(value).map(elements -> {
            if (!elements.isEmpty() && elements.stream().allMatch(CompositeData.class::isInstance)) {
                List<CompositeData> composites =
                        elements.stream().map(CompositeData.class::cast).toList();
                return new CompositeArrayLevel(composites, 0);
            }
            return new ValueListLevel(sequenceTitle(value, elements.size()), elements);
        });
    }

    /** The elements of a one-dimensional array, of a collection, or a map's entries. */
    private static Optional<List<Object>> elements(Object value) {
        if (value.getClass().isArray() && !value.getClass().getComponentType().isArray()) {
            List<Object> elements = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                elements.add(Array.get(value, i));
            }
            return Optional.of(elements);
        }
        if (value instanceof Collection<?> collection) {
            return Optional.of(new ArrayList<>(collection));
        }
        if (value instanceof Map<?, ?> map) {
            return Optional.of(new ArrayList<>(map.entrySet()));
        }
        return Optional.empty();
    }

    private static String sequenceTitle(Object value, int size) {
        if (value.getClass().isArray()) {
            return ValueText.summary(value);
        }
        return value.getClass().getSimpleName() + "[" + size + "]";
    }
}
