package com.druvu.jconsole.core.mbeans;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** {@link MBeanValues#arrayToString}: every array kind on one line, anything else as itself, null included. */
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
}
