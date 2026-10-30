package com.druvu.jconsole.console;

import static org.assertj.core.api.Assertions.assertThat;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests for {@link ConsoleRenderer} — plain-text (never HTML) rendering of JMX return values. */
public class ConsoleRendererTest {

    private static CompositeType rowType() throws OpenDataException {
        return new CompositeType(
                "Row",
                "a row",
                new String[] {"name", "count"},
                new String[] {"the name", "the count"},
                new OpenType<?>[] {SimpleType.STRING, SimpleType.INTEGER});
    }

    private static CompositeDataSupport row(CompositeType t, String name, int count) throws OpenDataException {
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
    public void compositeRendersKeyValueBlockNoHtml() throws OpenDataException {
        String out = ConsoleRenderer.render(row(rowType(), "alpha", 7));
        Assert.assertTrue(out.contains("name = alpha"), out);
        Assert.assertTrue(out.contains("count = 7"), out);
        Assert.assertFalse(out.contains("<"), "must be plain text, no HTML: " + out);
    }

    @Test
    public void tabularRendersNumberedRowsSortedByIndex() throws OpenDataException {
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

    @Test
    public void arrayOfCompositesRendersElementByElement() throws OpenDataException {
        CompositeType t = rowType();

        String out = ConsoleRenderer.render(new CompositeData[] {row(t, "alpha", 1), row(t, "beta", 2)});

        assertThat(out)
                .isEqualTo(String.join(
                        "\n", "[0]", "  count = 1", "  name = alpha", "[1]", "  count = 2", "  name = beta"));
    }

    @Test
    public void compositeFieldHoldingCompositesOpensANestedBlock() throws OpenDataException {
        CompositeType rowType = rowType();
        CompositeType holderType = new CompositeType(
                "Holder", "holds rows", new String[] {"rows"}, new String[] {"the rows"}, new OpenType<?>[] {
                    new ArrayType<CompositeData>(1, rowType)
                });
        CompositeDataSupport holder = new CompositeDataSupport(
                holderType, new String[] {"rows"}, new Object[] {new CompositeData[] {row(rowType, "alpha", 1)}});

        assertThat(ConsoleRenderer.render(holder))
                .isEqualTo(String.join("\n", "rows = ", "  [0]", "    count = 1", "    name = alpha"));
    }
}
