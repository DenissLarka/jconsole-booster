package com.druvu.jconsole.fx.tab.mbeans.viewers;

import com.druvu.jconsole.core.mbeans.MBeanValues;
import java.lang.reflect.Array;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

/**
 * One line of text for an MBean value in a table cell.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class ValueText {

    private ValueText() {}

    /**
     * A short summary for the values the viewer opens ({@code int[3]}, the open type's name), the value's own text for
     * the others; empty for {@code null}.
     */
    public static String summary(Object value) {
        return switch (value) {
            case null -> "";
            case CompositeData composite -> composite.getCompositeType().getTypeName();
            case TabularData tabular -> tabular.getTabularType().getTypeName();
            default -> {
                if (value.getClass().isArray()) {
                    String component =
                            MBeanValues.getArrayClassName(value.getClass().getName());
                    yield component + "[" + Array.getLength(value) + "]";
                }
                yield value.toString();
            }
        };
    }

    /** The value's full text, arrays element by element; empty for {@code null}. */
    public static String plain(Object value) {
        return value == null ? "" : MBeanValues.arrayToString(value);
    }
}
