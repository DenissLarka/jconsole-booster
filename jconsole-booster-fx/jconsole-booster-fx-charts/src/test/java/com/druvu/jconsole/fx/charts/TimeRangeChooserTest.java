package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Checks that one chooser drives the time range of several charts, both ways. */
public class TimeRangeChooserTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void chartsTakeTheChosenRange() {
        List<TimeRange> ranges = FxTestToolkit.call(() -> {
            TimeSeriesChart memory = new TimeSeriesChart();
            TimeSeriesChart threads = new TimeSeriesChart();
            memory.setTimeRange(TimeRange.ONE_HOUR);
            TimeRangeChooser chooser = new TimeRangeChooser(memory, threads);
            TimeRange memoryAtFirst = memory.getTimeRange();
            chooser.setValue(TimeRange.FIVE_MINUTES);
            return List.of(memoryAtFirst, memory.getTimeRange(), threads.getTimeRange());
        });

        assertThat(ranges).containsExactly(TimeRange.ALL, TimeRange.FIVE_MINUTES, TimeRange.FIVE_MINUTES);
    }

    @Test
    public void aRangePickedOnOneChartMovesTheOthers() {
        List<TimeRange> ranges = FxTestToolkit.call(() -> {
            TimeSeriesChart memory = new TimeSeriesChart();
            TimeSeriesChart threads = new TimeSeriesChart();
            TimeRangeChooser chooser = new TimeRangeChooser(memory, threads);
            memory.setTimeRange(TimeRange.SIX_HOURS);
            return List.of(chooser.getValue(), threads.getTimeRange());
        });

        assertThat(ranges).containsOnly(TimeRange.SIX_HOURS);
    }

    @Test
    public void aRemovedChartGoesItsOwnWay() {
        List<TimeRange> ranges = FxTestToolkit.call(() -> {
            TimeSeriesChart memory = new TimeSeriesChart();
            TimeRangeChooser chooser = new TimeRangeChooser();
            chooser.addChart(memory);
            chooser.removeChart(memory);
            chooser.setValue(TimeRange.ONE_DAY);
            return List.of(memory.getTimeRange());
        });

        assertThat(ranges).containsExactly(TimeRange.ALL);
    }

    @Test
    public void offersEveryRange() {
        List<TimeRange> offered = FxTestToolkit.call(() -> List.copyOf(new TimeRangeChooser().getItems()));

        assertThat(offered).containsExactly(TimeRange.values());
    }
}
