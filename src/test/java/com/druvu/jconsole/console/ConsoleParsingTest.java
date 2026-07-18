package com.druvu.jconsole.console;

import com.druvu.jconsole.console.ConsoleMain.OpResolution;
import java.util.List;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests for {@link ConsoleMain}'s pure parsing helpers: the quote-aware tokenizer and operation resolution. */
public class ConsoleParsingTest {

    // ----- tokenize -----

    @Test
    public void plainWhitespaceSplit() {
        Assert.assertEquals(ConsoleMain.tokenize("call 0 a b"), List.of("call", "0", "a", "b"));
    }

    @Test
    public void collapsesRepeatedAndEdgeWhitespace() {
        Assert.assertEquals(ConsoleMain.tokenize("  spaced   out  "), List.of("spaced", "out"));
    }

    @Test
    public void doubleQuotedSpanIsOneToken() {
        Assert.assertEquals(ConsoleMain.tokenize("call setName \"John Doe\""), List.of("call", "setName", "John Doe"));
    }

    @Test
    public void emptyQuotesYieldEmptyToken() {
        Assert.assertEquals(ConsoleMain.tokenize("a \"\" b"), List.of("a", "", "b"));
    }

    // ----- resolveOp -----

    private static MBeanParameterInfo p(String type) {
        return new MBeanParameterInfo("x", type, "");
    }

    private static MBeanOperationInfo op(String name, String... paramTypes) {
        MBeanParameterInfo[] sig = new MBeanParameterInfo[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            sig[i] = p(paramTypes[i]);
        }
        return new MBeanOperationInfo(name, "", sig, "void", MBeanOperationInfo.UNKNOWN);
    }

    private List<MBeanOperationInfo> ops() {
        return List.of(
                op("reset"),
                op("setThreshold", "int"),
                op("ping", "java.lang.String"),
                op("ping", "java.lang.String", "int"));
    }

    @Test
    public void resolveByIndex() {
        OpResolution r = ConsoleMain.resolveOp(ops(), "1", 0);
        Assert.assertNotNull(r.op());
        Assert.assertEquals(r.op().getName(), "setThreshold");
    }

    @Test
    public void resolveByIndexOutOfRange() {
        OpResolution r = ConsoleMain.resolveOp(ops(), "99", 0);
        Assert.assertNull(r.op());
        Assert.assertTrue(r.error().contains("[99]"), r.error());
    }

    @Test
    public void resolveByUniqueName() {
        OpResolution r = ConsoleMain.resolveOp(ops(), "reset", 0);
        Assert.assertNotNull(r.op());
        Assert.assertEquals(r.op().getName(), "reset");
    }

    @Test
    public void resolveOverloadByArity() {
        OpResolution one = ConsoleMain.resolveOp(ops(), "ping", 1);
        Assert.assertNotNull(one.op());
        Assert.assertEquals(one.op().getSignature().length, 1);

        OpResolution two = ConsoleMain.resolveOp(ops(), "ping", 2);
        Assert.assertNotNull(two.op());
        Assert.assertEquals(two.op().getSignature().length, 2);
    }

    @Test
    public void resolveAmbiguousOverloadRefused() {
        // arg count matches neither ping overload → refuse and list candidates by index
        OpResolution r = ConsoleMain.resolveOp(ops(), "ping", 5);
        Assert.assertNull(r.op());
        Assert.assertTrue(r.error().contains("overloaded"), r.error());
    }

    @Test
    public void resolveUnknownName() {
        OpResolution r = ConsoleMain.resolveOp(ops(), "nope", 0);
        Assert.assertNull(r.op());
        Assert.assertTrue(r.error().contains("nope"), r.error());
    }
}
