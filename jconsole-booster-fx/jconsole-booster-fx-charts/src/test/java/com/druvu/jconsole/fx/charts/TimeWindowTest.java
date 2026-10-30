package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import javafx.scene.paint.Color;
import org.testng.annotations.Test;

/** Checks which stretch of time a chart shows for each range. */
public class TimeWindowTest {

    private static final long MINUTE = Duration.ofMinutes(1).toMillis();
    private static final long START = 1_000_000_000L;

    @Test
    public void aFixedRangeEndsAtTheNewestSample() {
        SampleStore store = samplesOver(30 * MINUTE);

        TimeWindow window = TimeWindow.of(TimeRange.FIVE_MINUTES, store, 0);

        assertThat(window.end()).isEqualTo(START + 30 * MINUTE);
        assertThat(window.span()).isEqualTo(5 * MINUTE);
    }

    @Test
    public void allSpansFromTheFirstToTheNewestSample() {
        SampleStore store = samplesOver(30 * MINUTE);

        TimeWindow window = TimeWindow.of(TimeRange.ALL, store, 0);

        assertThat(window).isEqualTo(new TimeWindow(START, START + 30 * MINUTE));
    }

    @Test
    public void allSpansAtLeastAMinute() {
        SampleStore store = samplesOver(10_000);

        TimeWindow window = TimeWindow.of(TimeRange.ALL, store, 0);

        assertThat(window.span()).isEqualTo(TimeWindow.MINIMUM_SPAN);
        assertThat(window.end()).isEqualTo(START + 10_000);
    }

    @Test
    public void anEmptyChartEndsNow() {
        long now = START + 99 * MINUTE;

        TimeWindow window = TimeWindow.of(TimeRange.TEN_MINUTES, new SampleStore(), now);

        assertThat(window).isEqualTo(new TimeWindow(now - 10 * MINUTE, now));
    }

    private static SampleStore samplesOver(long span) {
        SampleStore store = new SampleStore();
        store.addSeries(new Series("value", "Value", Color.RED, true));
        store.add(START, 1);
        store.add(START + span / 2, 2);
        store.add(START + span, 3);
        return store;
    }
}
