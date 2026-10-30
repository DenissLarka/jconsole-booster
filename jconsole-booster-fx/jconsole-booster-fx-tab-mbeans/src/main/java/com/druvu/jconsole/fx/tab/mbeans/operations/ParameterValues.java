package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.core.mbeans.MBeanValues;
import java.util.Arrays;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

/**
 * Turns what the user entered into the arguments of an invocation, and tells which operations the form can call.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class ParameterValues {

    private static final String BYTES = byte[].class.getName();

    private ParameterValues() {}

    /** Whether the form can supply every parameter: types made from text, or a file for a byte[] or String. */
    public static boolean isCallable(MBeanOperationInfo operation) {
        return Arrays.stream(operation.getSignature()).allMatch(ParameterValues::canSupply);
    }

    static boolean canSupply(MBeanParameterInfo parameter) {
        String type = parameter.getType();
        if (MBeanValues.isEditableType(type)) {
            return true;
        }
        return ParameterKind.of(parameter) instanceof ParameterKind.FileUpload && BYTES.equals(type);
    }

    /**
     * The arguments for the signature: a file's bytes as they are, text made into the parameter's type.
     *
     * @param types the parameter types, as the signature names them
     * @param entered per parameter a {@link String}, or a {@code byte[]} from a file
     * @throws ReflectiveOperationException a type has no constructor from text
     * @throws IllegalArgumentException the text does not make a value of the type
     */
    public static Object[] convert(String[] types, Object... entered) throws ReflectiveOperationException {
        Object[] arguments = new Object[entered.length];
        for (int i = 0; i < entered.length; i++) {
            arguments[i] = entered[i] instanceof byte[] bytes
                    ? bytes
                    : MBeanValues.createObjectFromString(types[i], (String) entered[i]);
        }
        return arguments;
    }
}
