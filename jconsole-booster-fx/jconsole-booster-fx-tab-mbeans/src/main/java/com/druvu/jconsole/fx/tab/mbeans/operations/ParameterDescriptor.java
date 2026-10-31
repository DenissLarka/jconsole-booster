package com.druvu.jconsole.fx.tab.mbeans.operations;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.management.MBeanParameterInfo;

/**
 * What a parameter's descriptor says about its value: the JMX fields {@code legalValues} and {@code defaultValue}. A
 * Standard MBean sets them with {@code @DescriptorKey} annotations, an Open MBean in its parameter info.
 *
 * @author Deniss Larka <br>
 *     on 31 Oct 2026
 */
final class ParameterDescriptor {

    private ParameterDescriptor() {}

    /**
     * The values the parameter accepts, in the descriptor's order; empty when the descriptor does not limit them. An
     * annotation gives an array, an Open MBean a set.
     */
    static List<String> legalValues(MBeanParameterInfo parameter) {
        Object field = parameter.getDescriptor().getFieldValue("legalValues");
        if (field instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        if (field != null && field.getClass().isArray()) {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < Array.getLength(field); i++) {
                values.add(String.valueOf(Array.get(field, i)));
            }
            return List.copyOf(values);
        }
        return List.of();
    }

    /** The value the input starts with when none is remembered; empty for none. */
    static String defaultValue(MBeanParameterInfo parameter) {
        Object field = parameter.getDescriptor().getFieldValue("defaultValue");
        return field == null ? "" : String.valueOf(field);
    }
}
