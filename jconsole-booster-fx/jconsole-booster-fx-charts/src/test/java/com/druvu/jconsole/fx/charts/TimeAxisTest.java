package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.testng.annotations.Test;

/** Checks the clock ticks of the time axis: their interval, their place, and the midnight date. */
public class TimeAxisTest {

    private static final double WIDTH = 600;

    @Test
    public void theIntervalIsTheSmallestRoundOneThatFits() {
        assertThat(TimeAxis.tickInterval(millis(Duration.ofMinutes(10)), WIDTH, 40))
                .isEqualTo(minutes(1));
        assertThat(TimeAxis.tickInterval(millis(Duration.ofHours(1)), WIDTH, 40))
                .isEqualTo(minutes(5));
        assertThat(TimeAxis.tickInterval(millis(Duration.ofDays(7)), WIDTH, 80)).isEqualTo(days(1));
        assertThat(TimeAxis.tickInterval(millis(Duration.ofDays(366)), WIDTH, 80))
                .isEqualTo(days(50));
    }

    @Test
    public void ticksFallOnWholeMinutesInsideTheWindow() {
        TimeWindow window = window("2026-10-30T10:00:30Z", "2026-10-30T10:10:30Z");

        List<TimeTick> ticks = TimeAxis.ticks(window, WIDTH, ZoneOffset.UTC);

        assertThat(ticks).hasSize(10);
        assertThat(ticks.getFirst().time()).isEqualTo(time("2026-10-30T10:01:00Z"));
        assertThat(ticks.getLast().time()).isEqualTo(time("2026-10-30T10:10:00Z"));
        assertThat(ticks).noneMatch(TimeTick::midnight);
    }

    @Test
    public void theMidnightTickCarriesTheDate() {
        TimeWindow window = window("2026-10-30T20:00:00Z", "2026-10-31T02:00:00Z");

        List<TimeTick> ticks = TimeAxis.ticks(window, WIDTH, ZoneOffset.UTC);

        assertThat(ticks.stream().filter(TimeTick::midnight).map(TimeTick::time))
                .containsExactly(time("2026-10-31T00:00:00Z"));
    }

    @Test
    public void ticksFollowTheLocalZone() {
        TimeWindow window = window("2026-10-30T20:00:00Z", "2026-10-31T02:00:00Z");

        List<TimeTick> ticks = TimeAxis.ticks(window, WIDTH, ZoneOffset.ofHours(2));

        assertThat(ticks.stream().filter(TimeTick::midnight).map(TimeTick::time))
                .containsExactly(time("2026-10-30T22:00:00Z"));
    }

    @Test
    public void longWindowsSpaceTheirTicksWider() {
        TimeWindow window = window("2026-10-29T00:00:00Z", "2026-10-31T00:00:00Z");

        List<TimeTick> ticks = TimeAxis.ticks(window, WIDTH, ZoneOffset.UTC);

        assertThat(ticks).hasSizeGreaterThan(1);
        assertThat(ticks.get(1).time() - ticks.get(0).time()).isEqualTo(millis(Duration.ofHours(12)));
    }

    private static TimeWindow window(String start, String end) {
        return new TimeWindow(time(start), time(end));
    }

    private static long time(String instant) {
        return Instant.parse(instant).toEpochMilli();
    }

    private static long millis(Duration duration) {
        return duration.toMillis();
    }

    private static long minutes(long count) {
        return Duration.ofMinutes(count).toMillis();
    }

    private static long days(long count) {
        return Duration.ofDays(count).toMillis();
    }
}
