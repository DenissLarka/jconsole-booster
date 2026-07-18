package com.druvu.jconsole.console;

import com.druvu.jconsole.util.Utilities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

/**
 * Renders JMX operation return values as plain text. The only open-type stringifier in the codebase is Swing
 * ({@code XOpenTypeViewer} and friends), so this is genuinely new, HTML-free code.
 *
 * <ul>
 *   <li>scalars → {@code String.valueOf}
 *   <li>{@code byte[]} → a {@code <n> bytes} length preview (never dumped; file-save is a future item)
 *   <li>other arrays/collections → {@link Utilities#arrayToString} (Swing-free, null-safe)
 *   <li>{@link CompositeData} → an indented {@code key = value} block
 *   <li>{@link TabularData} → numbered rows, each a composite, rows sorted by the TabularType index names
 * </ul>
 *
 * Recursion (nested composites/tabulars) is depth-capped defensively.
 */
final class ConsoleRenderer {

    private static final int MAX_DEPTH = 8;
    private static final String INDENT = "  ";

    private ConsoleRenderer() {}

    static String render(Object value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value, 0);
        return sb.toString();
    }

    private static void append(StringBuilder sb, Object value, int depth) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (depth >= MAX_DEPTH) {
            sb.append("…");
            return;
        }
        if (value instanceof byte[] b) {
            sb.append(b.length).append(" bytes");
            return;
        }
        if (value instanceof CompositeData cd) {
            appendComposite(sb, cd, depth);
            return;
        }
        if (value instanceof TabularData td) {
            appendTabular(sb, td, depth);
            return;
        }
        if (value.getClass().isArray()) {
            sb.append(Utilities.arrayToString(value));
            return;
        }
        sb.append(String.valueOf(value));
    }

    private static void appendComposite(StringBuilder sb, CompositeData cd, int depth) {
        CompositeType type = cd.getCompositeType();
        String indent = INDENT.repeat(depth);
        boolean first = true;
        for (String key : type.keySet()) {
            if (!first) {
                sb.append('\n');
            }
            first = false;
            sb.append(indent).append(key).append(" = ");
            Object v = cd.get(key);
            if (v instanceof CompositeData || v instanceof TabularData) {
                sb.append('\n');
                append(sb, v, depth + 1);
            } else {
                append(sb, v, depth + 1);
            }
        }
    }

    private static void appendTabular(StringBuilder sb, TabularData td, int depth) {
        TabularType type = td.getTabularType();
        @SuppressWarnings("unchecked")
        List<CompositeData> rows = new ArrayList<>((java.util.Collection<CompositeData>) td.values());
        rows.sort(indexComparator(type));
        String indent = INDENT.repeat(depth);
        if (rows.isEmpty()) {
            sb.append(indent).append("(empty)");
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(indent).append('[').append(i).append("]\n");
            append(sb, rows.get(i), depth + 1);
        }
    }

    /**
     * Comparator over the TabularType's index names — logic copied from {@code XOpenTypeViewer.TabularDataComparator}.
     */
    private static Comparator<CompositeData> indexComparator(TabularType type) {
        List<String> indexNames = type.getIndexNames();
        return (o1, o2) -> {
            for (String key : indexNames) {
                Object c1 = o1.get(key);
                Object c2 = o2.get(key);
                if (c1 instanceof Comparable && c2 instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    int result = ((Comparable<Object>) c1).compareTo(c2);
                    if (result != 0) {
                        return result;
                    }
                }
            }
            return 0;
        };
    }
}
