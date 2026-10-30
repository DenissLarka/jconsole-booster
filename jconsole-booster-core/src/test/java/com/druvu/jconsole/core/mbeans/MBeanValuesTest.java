package com.druvu.jconsole.core.mbeans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.testng.annotations.Test;

/**
 * {@link MBeanValues}: arrays on one line; text made into the declared type, a concrete number type included, and the
 * kind left to the text only for {@code Number}.
 */
public class MBeanValuesTest {

    @Test
    public void testArrayToString() {
        assertThat(MBeanValues.arrayToString(new Object[] {"a", "b"})).isEqualTo("[a, b]");
        assertThat(MBeanValues.arrayToString(new boolean[] {true, false})).isEqualTo("[true, false]");
        assertThat(MBeanValues.arrayToString(new byte[] {1, 2})).isEqualTo("[1, 2]");
        assertThat(MBeanValues.arrayToString(new char[] {'a', 'b'})).isEqualTo("[a, b]");
        assertThat(MBeanValues.arrayToString(new double[] {1.1, 2.2})).isEqualTo("[1.1, 2.2]");
        assertThat(MBeanValues.arrayToString(new float[] {1.1f, 2.2f})).isEqualTo("[1.1, 2.2]");
        assertThat(MBeanValues.arrayToString(new int[] {1, 2})).isEqualTo("[1, 2]");
        assertThat(MBeanValues.arrayToString(new long[] {1L, 2L})).isEqualTo("[1, 2]");
        assertThat(MBeanValues.arrayToString(new short[] {1, 2})).isEqualTo("[1, 2]");
        assertThat(MBeanValues.arrayToString("test")).isEqualTo("test");
        assertThat(MBeanValues.arrayToString(123)).isEqualTo("123");
        assertThat(MBeanValues.arrayToString(null)).isEqualTo("null");
    }

    @Test
    public void aConcreteNumberTypeGetsItsOwnType() throws ReflectiveOperationException {
        assertThat(MBeanValues.createObjectFromString("java.lang.Long", "7")).isEqualTo(7L);
        assertThat(MBeanValues.createObjectFromString("java.lang.Short", "7")).isEqualTo((short) 7);
        assertThat(MBeanValues.createObjectFromString("java.lang.Double", "1.5"))
                .isEqualTo(1.5d);
        assertThat(MBeanValues.createObjectFromString("java.math.BigDecimal", "1.50"))
                .isEqualTo(new BigDecimal("1.50"));
        assertThat(MBeanValues.createObjectFromString("long", "7")).isEqualTo(7L);
    }

    @Test
    public void aPlainNumberTakesItsKindFromTheText() throws ReflectiveOperationException {
        assertThat(MBeanValues.createObjectFromString("java.lang.Number", "7")).isEqualTo(7);
        assertThat(MBeanValues.createObjectFromString("java.lang.Number", "7L")).isEqualTo(7L);
        assertThat(MBeanValues.createObjectFromString("java.lang.Number", "1.5"))
                .isEqualTo(1.5d);
    }

    @Test
    public void textThatIsNoNumberIsRefusedWithTheReason() {
        assertThatThrownBy(() -> MBeanValues.createObjectFromString("java.lang.Long", "seven"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seven");
    }
}
