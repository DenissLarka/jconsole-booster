package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javafx.scene.paint.Color;
import org.testng.annotations.Test;

/** Checks how samples, series and gaps are stored. */
public class SampleStoreTest {

    @Test
    public void keepsOneValuePerSeriesForEachTime() {
        SampleStore store = twoSeries();
        store.add(1000, 1, 10);
        store.add(2000, 2, 20);

        assertThat(store.size()).isEqualTo(2);
        assertThat(store.time(1)).isEqualTo(2000);
        assertThat(store.series().get(0).value(1)).isEqualTo(2);
        assertThat(store.series().get(1).value(1)).isEqualTo(20);
        assertThat(store.find("used").orElseThrow().lastValue()).hasValue(2);
    }

    @Test
    public void rejectsAWrongNumberOfValues() {
        SampleStore store = twoSeries();

        assertThatThrownBy(() -> store.add(1000, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.size()).isZero();
    }

    @Test
    public void rejectsADuplicateKey() {
        SampleStore store = twoSeries();

        assertThatThrownBy(() -> store.addSeries(new Series("used", "Used again", Color.GREEN, true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void rejectsASeriesAddedAfterValues() {
        SampleStore store = twoSeries();
        store.add(1000, 1, 10);

        assertThatThrownBy(() -> store.addSeries(new Series("late", "Late", Color.GREEN, true)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void aGapBreaksEverySeries() {
        SampleStore store = twoSeries();
        store.add(1000, 1, 10);
        store.addGap(2000);

        assertThat(store.size()).isEqualTo(2);
        assertThat(store.series())
                .allSatisfy(
                        series -> assertThat(SampleStore.isGap(series.value(1))).isTrue());
        assertThat(store.find("used").orElseThrow().lastValue()).isEmpty();
    }

    @Test
    public void findsTheFirstSampleOfAWindow() {
        SampleStore store = twoSeries();
        store.add(1000, 1, 10);
        store.add(2000, 2, 20);
        store.add(3000, 3, 30);

        assertThat(store.firstIndexAtOrAfter(0)).isZero();
        assertThat(store.firstIndexAtOrAfter(2000)).isEqualTo(1);
        assertThat(store.firstIndexAtOrAfter(2500)).isEqualTo(2);
        assertThat(store.firstIndexAtOrAfter(9000)).isEqualTo(3);
    }

    private static SampleStore twoSeries() {
        SampleStore store = new SampleStore();
        store.addSeries(new Series("used", "Used", Color.RED, true));
        store.addSeries(new Series("max", "Max", Color.BLUE, false));
        return store;
    }
}
