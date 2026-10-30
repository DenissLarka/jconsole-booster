package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javafx.scene.paint.Color;
import org.testng.annotations.Test;

/** Checks how the samples of a line collapse when more than four share a pixel. */
public class DecimatorTest {

    private static final long SECOND = 1000;

    @Test
    public void fewSamplesPerPixelAreAllKept() {
        SampleStore store = ramp(100);

        assertThat(Decimator.points(store, only(store), 0, 100)).hasSize(100);
    }

    @Test
    public void theLineStartsAtTheFirstVisibleSample() {
        SampleStore store = ramp(100);

        List<PlotPoint> points = Decimator.points(store, only(store), 40, 100);

        assertThat(points).hasSize(60);
        assertThat(points.getFirst()).isEqualTo(new PlotPoint(40 * SECOND, 40));
    }

    @Test
    public void samplesCollapseFromFourPerPixel() {
        SampleStore justBelow = ramp(399);
        SampleStore atThreshold = ramp(400);

        assertThat(Decimator.points(justBelow, only(justBelow), 0, 100)).hasSize(399);
        assertThat(Decimator.points(atThreshold, only(atThreshold), 0, 100)).hasSizeLessThan(400);
    }

    @Test
    public void hoursOfSamplesCollapseAndKeepBothEnds() {
        SampleStore store = ramp(10_000);

        List<PlotPoint> points = Decimator.points(store, only(store), 0, 100);

        assertThat(points).hasSizeLessThanOrEqualTo(4 * 100);
        assertThat(points.getFirst()).isEqualTo(new PlotPoint(0, 0));
        assertThat(points.getLast()).isEqualTo(new PlotPoint(9_999 * SECOND, 9_999));
    }

    @Test
    public void peaksInsideAPixelAreKept() {
        SampleStore store = flatWith(1000, 555, 999);
        Series series = only(store);

        List<PlotPoint> points = Decimator.points(store, series, 0, 10);

        assertThat(points).hasSizeLessThan(1000).contains(new PlotPoint(555 * SECOND, 999));
    }

    @Test
    public void aGapInsideACollapsedPixelStays() {
        SampleStore store = new SampleStore();
        store.addSeries(new Series("value", "Value", Color.RED, true));
        for (int i = 0; i < 1000; i++) {
            if (i == 500) {
                store.addGap(i * SECOND);
            } else {
                store.add(i * SECOND, 10);
            }
        }

        List<PlotPoint> points = Decimator.points(store, only(store), 0, 10);

        assertThat(points).hasSizeLessThan(1000).filteredOn(PlotPoint::isGap).hasSize(1);
        assertThat(points).contains(new PlotPoint(499 * SECOND, 10), new PlotPoint(501 * SECOND, 10));
    }

    private static SampleStore ramp(int count) {
        SampleStore store = new SampleStore();
        store.addSeries(new Series("value", "Value", Color.RED, true));
        for (int i = 0; i < count; i++) {
            store.add(i * SECOND, i);
        }
        return store;
    }

    private static SampleStore flatWith(int count, int peakIndex, long peak) {
        SampleStore store = new SampleStore();
        store.addSeries(new Series("value", "Value", Color.RED, true));
        for (int i = 0; i < count; i++) {
            store.add(i * SECOND, i == peakIndex ? peak : 10);
        }
        return store;
    }

    private static Series only(SampleStore store) {
        return store.series().getFirst();
    }
}
