package com.druvu.jconsole.fx.charts;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * The clock ticks along a chart's time axis: round intervals of local time (1, 5, 10, 30 minutes; 1, 3, 6, 12 hours;
 * days), spaced so that their labels do not crowd.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class TimeAxis {

    /** The least room between two ticks, in pixels; ticks over three hours apart carry dates and need twice as much. */
    static final double MIN_TICK_GAP = 40;

    private static final long DAY = Duration.ofDays(1).toMillis();
    private static final long WIDE_LABEL_INTERVAL = Duration.ofHours(3).toMillis();
    private static final List<Long> ROUND_INTERVALS = List.of(
            Duration.ofMinutes(1).toMillis(),
            Duration.ofMinutes(5).toMillis(),
            Duration.ofMinutes(10).toMillis(),
            Duration.ofMinutes(30).toMillis(),
            Duration.ofHours(1).toMillis(),
            Duration.ofHours(3).toMillis(),
            Duration.ofHours(6).toMillis(),
            Duration.ofHours(12).toMillis(),
            DAY);

    private TimeAxis() {}

    /**
     * The ticks inside {@code window} drawn {@code width} pixels wide, on whole intervals of local time in
     * {@code zone}.
     */
    static List<TimeTick> ticks(TimeWindow window, double width, ZoneId zone) {
        long interval = tickInterval(window.span(), width, MIN_TICK_GAP);
        if (interval > WIDE_LABEL_INTERVAL) {
            interval = tickInterval(window.span(), width, 2 * MIN_TICK_GAP);
        }
        long zoneOffset =
                zone.getRules().getOffset(Instant.ofEpochMilli(window.end())).getTotalSeconds() * 1000L;
        List<TimeTick> ticks = new ArrayList<>();
        // the first whole interval of local time after the window starts
        long sinceStart = interval - Math.floorMod(window.start() + zoneOffset, interval);
        while (sinceStart < window.span()) {
            long time = window.start() + sinceStart;
            ticks.add(new TimeTick(time, Math.floorMod(time + zoneOffset, DAY) == 0));
            sinceStart += interval;
        }
        return ticks;
    }

    /** The smallest round interval that leaves at least {@code minGap} pixels between two ticks. */
    static long tickInterval(long span, double width, double minGap) {
        long wanted = (long) (span * minGap / width);
        for (long interval : ROUND_INTERVALS) {
            if (wanted < interval) {
                return interval;
            }
        }
        // a day or more: whole days, rounded up to one significant digit
        return ValueScale.roundUp(wanted / DAY) * DAY;
    }
}
