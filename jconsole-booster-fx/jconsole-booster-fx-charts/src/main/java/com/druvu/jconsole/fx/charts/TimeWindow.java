package com.druvu.jconsole.fx.charts;

import java.time.Duration;

/**
 * The stretch of time a chart shows: it ends at the newest sample (or now, before the first one) and spans the chosen
 * range.
 *
 * @param start the oldest time shown, in epoch milliseconds
 * @param end the newest time shown, in epoch milliseconds
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record TimeWindow(long start, long end) {

    /** The shortest window: a chart showing all of its data still spans a minute. */
    static final long MINIMUM_SPAN = Duration.ofMinutes(1).toMillis();

    static TimeWindow of(TimeRange range, SampleStore store, long now) {
        if (store.size() == 0) {
            long span = range.span().map(Duration::toMillis).orElse(MINIMUM_SPAN);
            return new TimeWindow(now - span, now);
        }
        long first = store.time(0);
        long newest = store.time(store.size() - 1);
        long span = range.span().map(Duration::toMillis).orElse(Math.max(newest - first, MINIMUM_SPAN));
        return new TimeWindow(newest - span, newest);
    }

    long span() {
        return end - start;
    }
}
