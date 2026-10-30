package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LongSummaryStatistics;
import java.util.stream.LongStream;
import javafx.scene.paint.Color;
import org.testng.annotations.Test;

/** Checks the value axis: its rounded bounds, its ticks, and which series set it. */
public class ValueScaleTest {

    @Test
    public void roundUpGoesToTheNextOneDigitNumber() {
        assertThat(ValueScale.roundUp(1)).isEqualTo(2);
        assertThat(ValueScale.roundUp(37)).isEqualTo(40);
        assertThat(ValueScale.roundUp(95)).isEqualTo(100);
        assertThat(ValueScale.roundUp(100)).isEqualTo(200);
        assertThat(ValueScale.roundUp(999)).isEqualTo(1000);
    }

    @Test
    public void roundDownCutsToTheFirstDigit() {
        assertThat(ValueScale.roundDown(5)).isEqualTo(5);
        assertThat(ValueScale.roundDown(37)).isEqualTo(30);
        assertThat(ValueScale.roundDown(1234)).isEqualTo(1000);
    }

    @Test
    public void anEmptyScaleRunsFromZeroToTwo() {
        ValueScale scale = ValueScale.of(new LongSummaryStatistics());

        assertThat(scale.min()).isZero();
        assertThat(scale.max()).isEqualTo(2);
        assertThat(scale.ticks()).containsExactly(0L, 1L, 2L);
    }

    @Test
    public void aLowFarBelowTheTopStartsAtZero() {
        ValueScale scale = ValueScale.of(values(5, 37));

        assertThat(scale.min()).isZero();
        assertThat(scale.ticks()).containsExactly(0L, 10L, 20L, 30L, 40L);
    }

    @Test
    public void aLowNearTheTopIsRoundedDown() {
        ValueScale scale = ValueScale.of(values(12, 37));

        assertThat(scale.min()).isEqualTo(10);
        assertThat(scale.ticks()).containsExactly(10L, 20L, 30L, 40L);
    }

    @Test
    public void theLeadingDigitOfTheTopSetsTheSteps() {
        assertThat(ValueScale.of(values(0, 850)).ticks()).containsExactly(0L, 300L, 600L, 900L);
        assertThat(ValueScale.of(values(0, 55)).ticks()).containsExactly(0L, 10L, 20L, 30L, 40L, 50L, 60L);
        assertThat(ValueScale.of(values(0, 7_500)).ticks()).hasSize(9);
    }

    @Test
    public void bytesScaleFollowsTheFirstSeriesOnly() {
        SampleStore store = usedAndMax(true);

        ValueScale scale = ValueScale.forVisible(store, 0, ChartUnit.BYTES);

        assertThat(scale.max()).isEqualTo(300);
    }

    @Test
    public void otherUnitsScaleToEveryDrawnSeries() {
        SampleStore store = usedAndMax(true);

        ValueScale scale = ValueScale.forVisible(store, 0, ChartUnit.NONE);

        assertThat(scale.max()).isEqualTo(2000);
    }

    @Test
    public void hiddenSeriesDoNotSetTheScale() {
        SampleStore store = usedAndMax(false);

        ValueScale scale = ValueScale.forVisible(store, 0, ChartUnit.NONE);

        assertThat(scale.max()).isEqualTo(300);
    }

    @Test
    public void gapsAndSamplesBeforeTheWindowAreIgnored() {
        SampleStore store = new SampleStore();
        store.addSeries(new Series("used", "Used", Color.RED, true));
        store.add(1000, 5_000);
        store.add(2000, 150);
        store.addGap(3000);
        store.add(4000, 120);

        ValueScale scale = ValueScale.forVisible(store, 1, ChartUnit.NONE);

        assertThat(scale.min()).isEqualTo(100);
        assertThat(scale.max()).isEqualTo(200);
    }

    @Test
    public void fractionPutsTheBoundsAtZeroAndOne() {
        ValueScale scale = ValueScale.of(values(0, 37));

        assertThat(scale.fraction(0)).isZero();
        assertThat(scale.fraction(20)).isEqualTo(0.5);
        assertThat(scale.fraction(40)).isEqualTo(1.0);
        assertThat(scale.contains(41)).isFalse();
    }

    private static LongSummaryStatistics values(long... values) {
        return LongStream.of(values).summaryStatistics();
    }

    private static SampleStore usedAndMax(boolean maxPlotted) {
        SampleStore store = new SampleStore();
        store.addSeries(new Series("used", "Used", Color.RED, true));
        store.addSeries(new Series("max", "Max", Color.BLUE, maxPlotted));
        store.add(1000, 100, 1000);
        store.add(2000, 250, 1000);
        return store;
    }
}
