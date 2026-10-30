package com.druvu.jconsole.fx.tab.mbeans.viewers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** How the viewer splits structured values into levels the user opens one at a time. */
public class ValueLevelTest {

    private CompositeType pointType;
    private CompositeType rowType;
    private TabularType tableType;

    @BeforeClass
    public void types() throws OpenDataException {
        pointType = new CompositeType(
                "Point", "A point", new String[] {"x", "y"}, new String[] {"x", "y"}, new OpenType<?>[] {
                    SimpleType.INTEGER, SimpleType.INTEGER
                });
        rowType = new CompositeType(
                "Row",
                "A row",
                new String[] {"name", "count", "alpha"},
                new String[] {"name", "count", "alpha"},
                new OpenType<?>[] {SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING});
        tableType = new TabularType("Rows", "Rows by name", rowType, new String[] {"name"});
    }

    @Test
    public void aCompositeIsOneRowPerItem() throws OpenDataException {
        ValueLevel level = ValueLevel.of(point(1, 2)).orElseThrow();

        assertThat(level).isInstanceOf(CompositeLevel.class);
        assertThat(level.title()).isEqualTo("Point");
        assertThat(level.columns()).containsExactly("Name", "Value");
        assertThat(level.rows()).containsExactly(List.of("x", 1), List.of("y", 2));
    }

    @Test
    public void aTableHasItsIndexFirstAndRowsSortedByIt() throws OpenDataException {
        TabularDataSupport table = new TabularDataSupport(tableType);
        table.put(row("beta", 2, "b,b"));
        table.put(row("alpha", 1, "a"));

        TabularLevel level = (TabularLevel) ValueLevel.of(table).orElseThrow();

        assertThat(level.columns()).containsExactly("name", "alpha", "count");
        assertThat(level.rows()).containsExactly(List.of("alpha", "a", 1), List.of("beta", "b,b", 2));
        assertThat(level.csv()).isEqualTo("name,alpha,count\r\nalpha,a,1\r\nbeta,\"b,b\",2\r\n");
    }

    @Test
    public void anArrayOfCompositesIsWalkedOneByOne() throws OpenDataException {
        CompositeArrayLevel level = (CompositeArrayLevel)
                ValueLevel.of(new CompositeData[] {point(1, 2), point(3, 4)}).orElseThrow();

        assertThat(level.title()).isEqualTo("Point[2]");
        assertThat(level.hasPrevious()).isFalse();
        assertThat(level.hasNext()).isTrue();

        CompositeArrayLevel second = level.next();

        assertThat(second.rows()).containsExactly(List.of("x", 3), List.of("y", 4));
        assertThat(second.hasNext()).isFalse();
        assertThat(second.previous()).isEqualTo(level);
    }

    @Test
    public void arraysCollectionsAndMapsAreListsOfValues() {
        ValueLevel array = ValueLevel.of(new long[] {1, 2, 3}).orElseThrow();
        ValueLevel collection = ValueLevel.of(List.of("a", 1)).orElseThrow();
        ValueLevel map = ValueLevel.of(Map.of("k", "v")).orElseThrow();

        assertThat(array.title()).isEqualTo("long[3]");
        assertThat(array.columns()).containsExactly("Value");
        assertThat(array.rows()).containsExactly(List.of(1L), List.of(2L), List.of(3L));
        assertThat(collection.rows()).containsExactly(List.of("a"), List.of(1));
        assertThat(map.rows()).hasSize(1);
    }

    @Test
    public void aStructuredPartOpensAndAnEmptyOneDoesNot() throws OpenDataException {
        ValueLevel list =
                ValueLevel.of(List.of(point(1, 2), "plain", new int[0])).orElseThrow();

        List<Object> cells = list.rows().stream().map(List::getFirst).toList();

        assertThat(cells.get(0)).isInstanceOf(CompositeLevel.class);
        assertThat(cells.get(1)).isEqualTo("plain");
        assertThat(cells.get(2)).isInstanceOf(int[].class);
        assertThat(ValueLevel.cell(null)).isEqualTo("");
    }

    @Test
    public void plainValuesDoNotOpen() {
        assertThat(ValueLevel.of(42)).isEmpty();
        assertThat(ValueLevel.of("text")).isEmpty();
        assertThat(ValueLevel.of(null)).isEmpty();
        assertThat(ValueLevel.of(new int[][] {{1}}))
                .as("more than one dimension")
                .isEmpty();
    }

    @Test
    public void summariesAreShort() throws OpenDataException {
        assertThat(ValueText.summary(point(1, 2))).isEqualTo("Point");
        assertThat(ValueText.summary(new int[2])).isEqualTo("int[2]");
        assertThat(ValueText.summary(null)).isEmpty();
        assertThat(ValueText.summary(5)).isEqualTo("5");
        assertThat(ValueText.plain(new int[] {1, 2})).isEqualTo("[1, 2]");
    }

    private CompositeData point(int x, int y) throws OpenDataException {
        return new CompositeDataSupport(pointType, new String[] {"x", "y"}, new Object[] {x, y});
    }

    private CompositeData row(String name, int count, String alpha) throws OpenDataException {
        return new CompositeDataSupport(
                rowType, new String[] {"name", "count", "alpha"}, new Object[] {name, count, alpha});
    }
}
