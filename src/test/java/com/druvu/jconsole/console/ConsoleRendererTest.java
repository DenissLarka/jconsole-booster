package com.druvu.jconsole.console;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests for {@link ConsoleRenderer} — plain-text (never HTML) rendering of JMX return values. */
public class ConsoleRendererTest {

    private static CompositeType rowType() throws Exception {
        return new CompositeType(
                "Row",
                "a row",
                new String[] {"name", "count"},
                new String[] {"the name", "the count"},
                new OpenType<?>[] {SimpleType.STRING, SimpleType.INTEGER});
    }

    private static CompositeDataSupport row(CompositeType t, String name, int count) throws Exception {
        return new CompositeDataSupport(t, new String[] {"name", "count"}, new Object[] {name, count});
    }

    @Test
    public void nullRendersAsNull() {
        Assert.assertEquals(ConsoleRenderer.render(null), "null");
    }

    @Test
    public void scalarRendersViaValueOf() {
        Assert.assertEquals(ConsoleRenderer.render(42), "42");
        Assert.assertEquals(ConsoleRenderer.render("hello"), "hello");
    }

    @Test
    public void primitiveArrayRenders() {
        Assert.assertEquals(ConsoleRenderer.render(new int[] {1, 2, 3}), "[1, 2, 3]");
    }

    @Test
    public void byteArrayRendersAsLength() {
        Assert.assertEquals(ConsoleRenderer.render(new byte[10]), "10 bytes");
    }

    @Test
    public void compositeRendersKeyValueBlockNoHtml() throws Exception {
        String out = ConsoleRenderer.render(row(rowType(), "alpha", 7));
        Assert.assertTrue(out.contains("name = alpha"), out);
        Assert.assertTrue(out.contains("count = 7"), out);
        Assert.assertFalse(out.contains("<"), "must be plain text, no HTML: " + out);
    }

    @Test
    public void tabularRendersNumberedRowsSortedByIndex() throws Exception {
        CompositeType t = rowType();
        TabularType tt = new TabularType("Table", "rows", t, new String[] {"name"});
        TabularDataSupport td = new TabularDataSupport(tt);
        td.put(row(t, "beta", 2));
        td.put(row(t, "alpha", 1));

        String out = ConsoleRenderer.render(td);
        Assert.assertTrue(out.contains("[0]"), out);
        Assert.assertTrue(out.contains("[1]"), out);
        // sorted by the index name "name": alpha must come before beta
        Assert.assertTrue(out.indexOf("alpha") < out.indexOf("beta"), out);
        Assert.assertFalse(out.contains("<"), "must be plain text, no HTML: " + out);
    }
}
