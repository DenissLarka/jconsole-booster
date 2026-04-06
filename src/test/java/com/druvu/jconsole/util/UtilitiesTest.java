package com.druvu.jconsole.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UtilitiesTest {

    @Test
    public void testArrayToString() {
        Assert.assertEquals(Utilities.arrayToString(new Object[] {"a", "b"}), "[a, b]");
        Assert.assertEquals(Utilities.arrayToString(new boolean[] {true, false}), "[true, false]");
        Assert.assertEquals(Utilities.arrayToString(new byte[] {1, 2}), "[1, 2]");
        Assert.assertEquals(Utilities.arrayToString(new char[] {'a', 'b'}), "[a, b]");
        Assert.assertEquals(Utilities.arrayToString(new double[] {1.1, 2.2}), "[1.1, 2.2]");
        Assert.assertEquals(Utilities.arrayToString(new float[] {1.1f, 2.2f}), "[1.1, 2.2]");
        Assert.assertEquals(Utilities.arrayToString(new int[] {1, 2}), "[1, 2]");
        Assert.assertEquals(Utilities.arrayToString(new long[] {1L, 2L}), "[1, 2]");
        Assert.assertEquals(Utilities.arrayToString(new short[] {1, 2}), "[1, 2]");
        Assert.assertEquals(Utilities.arrayToString("test"), "test");
        Assert.assertEquals(Utilities.arrayToString(123), "123");
        Assert.assertEquals(Utilities.arrayToString(null), "null");
    }
}
