package com.druvu.jconsole.fx.tab.mbeans.viewers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

/**
 * Tabular data as one flat table: one row per composite, the index columns first in their declared order, then the
 * other items by name. Rows are sorted by their index values.
 *
 * @param title the table's type name
 * @param columns the column names, index columns first
 * @param entries the rows, sorted
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record TabularLevel(String title, List<String> columns, List<CompositeData> entries) implements ValueLevel {

    public TabularLevel {
        columns = List.copyOf(columns);
        entries = List.copyOf(entries);
    }

    static TabularLevel of(TabularData data) {
        TabularType type = data.getTabularType();
        List<String> index = type.getIndexNames();
        List<String> others = new ArrayList<>(type.getRowType().keySet());
        others.removeAll(index);
        Collections.sort(others);
        List<String> columns = new ArrayList<>(index);
        columns.addAll(others);
        List<CompositeData> entries = data.values().stream()
                .map(CompositeData.class::cast)
                .sorted(byIndex(index))
                .toList();
        return new TabularLevel(type.getTypeName(), columns, entries);
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public List<List<Object>> rows() {
        return table(ValueLevel::cell);
    }

    /** The whole table as CSV, a header line first, every cell as text. */
    public String csv() {
        List<List<String>> rows = table(ValueText::plain);
        return Csv.of(columns, rows);
    }

    private <T> List<List<T>> table(Function<Object, T> cell) {
        return entries.stream()
                .map(entry -> columns.stream()
                        .map(column -> cell.apply(entry.get(column)))
                        .toList())
                .toList();
    }

    /** Orders rows by their index values, the first index column first; values that do not compare are equal. */
    @SuppressWarnings("unchecked")
    private static Comparator<CompositeData> byIndex(List<String> index) {
        return (first, second) -> {
            for (String column : index) {
                Object a = first.get(column);
                Object b = second.get(column);
                if (a instanceof Comparable<?> comparable && b != null && a.getClass() == b.getClass()) {
                    int order = ((Comparable<Object>) comparable).compareTo(b);
                    if (order != 0) {
                        return order;
                    }
                }
            }
            return 0;
        };
    }
}
