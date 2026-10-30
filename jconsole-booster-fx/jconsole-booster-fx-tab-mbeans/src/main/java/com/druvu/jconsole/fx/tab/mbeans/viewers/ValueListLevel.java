package com.druvu.jconsole.fx.tab.mbeans.viewers;

import com.druvu.jconsole.core.text.Messages;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The elements of an array, a collection or a map, one per row.
 *
 * @param title the caption, such as {@code long[3]}
 * @param values the elements, {@code null} ones included
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record ValueListLevel(String title, List<Object> values) implements ValueLevel {

    public ValueListLevel {
        values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public List<String> columns() {
        return List.of(Messages.VALUE);
    }

    @Override
    public List<List<Object>> rows() {
        return values.stream().map(value -> List.of(ValueLevel.cell(value))).toList();
    }
}
