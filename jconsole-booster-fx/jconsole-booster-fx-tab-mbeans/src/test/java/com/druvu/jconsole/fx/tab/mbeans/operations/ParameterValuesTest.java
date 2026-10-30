package com.druvu.jconsole.fx.tab.mbeans.operations;

import static com.druvu.jconsole.fx.tab.mbeans.operations.ParameterKindTest.parameter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import org.testng.annotations.Test;

/** What the user entered becomes the invocation's arguments; the form calls only what it can supply. */
public class ParameterValuesTest {

    @Test
    public void textBecomesTheParametersType() throws ReflectiveOperationException {
        Object[] arguments = ParameterValues.convert(
                new String[] {"int", "long", "boolean", "java.lang.String", "char"}, "42", "7", "true", "text", "x");

        assertThat(arguments).containsExactly(42, 7L, true, "text", 'x');
    }

    @Test
    public void aFilesBytesPassAsTheyAre() throws ReflectiveOperationException {
        byte[] content = {1, 2, 3};

        assertThat(ParameterValues.convert(new String[] {"[B"}, (Object) content)[0])
                .isSameAs(content);
    }

    @Test
    public void textThatIsNoNumberIsRefused() {
        assertThatThrownBy(() -> ParameterValues.convert(new String[] {"int"}, "many"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void anOperationWithTextTypesIsCallable() {
        assertThat(ParameterValues.isCallable(operation(parameter("", "int"), parameter("", "java.lang.String"))))
                .isTrue();
        assertThat(ParameterValues.isCallable(operation())).isTrue();
    }

    @Test
    public void aTypeTheFormCannotMakeDisablesTheOperation() {
        assertThat(ParameterValues.isCallable(operation(parameter("", "javax.management.openmbean.CompositeData"))))
                .isFalse();
        assertThat(ParameterValues.isCallable(operation(parameter("Payload", "[B"))))
                .isFalse();
    }

    @Test
    public void aFileSuppliesABytesParameter() {
        assertThat(ParameterValues.isCallable(operation(parameter("CSV {{file:*.csv}}", "[B"))))
                .isTrue();
    }

    @Test
    public void voidReturnsNothingToShow() {
        assertThat(OperationCall.isVoid("void")).isTrue();
        assertThat(OperationCall.isVoid("java.lang.Void")).isTrue();
        assertThat(OperationCall.isVoid(null)).isTrue();
        assertThat(OperationCall.isVoid("int")).isFalse();
    }

    private static MBeanOperationInfo operation(MBeanParameterInfo... signature) {
        return new MBeanOperationInfo("op", "", signature, "void", MBeanOperationInfo.ACTION);
    }
}
