package com.druvu.jconsole.fx.tab.overview;

import java.util.OptionalDouble;

/**
 * The target process's CPU usage between two readings, in percent of all its processors: the CPU time the process used,
 * over the time that passed times the number of processors. Used on the update thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class CpuUsage {

    /**
     * The highest usage shown. The CPU time and the uptime are not read at the same instant, so the ratio can overshoot
     * 100%; the cap keeps the chart's scale at 0 to 100%.
     */
    static final double MAX_PERCENT = 99;

    /** CPU nanoseconds over elapsed milliseconds: 1,000,000 ns per ms, divided by 100 for a percentage. */
    private static final double NANOS_PER_MILLI_PERCENT = 10_000;

    /** The previous reading; an uptime of 0 means there is none yet. */
    private long previousCpuNanos;

    private long previousUptimeMillis;

    /**
     * Takes a reading.
     *
     * @param processCpuNanos the CPU time the process has used, in nanoseconds; negative when it is not measured
     * @param uptimeMillis how long the JVM has run, in milliseconds
     * @param processors the processors available to the JVM
     * @return the usage since the previous reading; empty for the first reading, when the CPU time is not measured, and
     *     when no time has passed
     */
    OptionalDouble next(long processCpuNanos, long uptimeMillis, int processors) {
        if (processCpuNanos < 0 || uptimeMillis <= 0) {
            return OptionalDouble.empty();
        }
        OptionalDouble usage = previousUptimeMillis > 0 && uptimeMillis > previousUptimeMillis
                ? OptionalDouble.of(
                        percent(processCpuNanos - previousCpuNanos, uptimeMillis - previousUptimeMillis, processors))
                : OptionalDouble.empty();
        previousCpuNanos = processCpuNanos;
        previousUptimeMillis = uptimeMillis;
        return usage;
    }

    /** How much of {@code processors} running for {@code elapsedMillis} the {@code cpuNanos} of CPU time filled. */
    static double percent(long cpuNanos, long elapsedMillis, int processors) {
        double percent = cpuNanos / (elapsedMillis * NANOS_PER_MILLI_PERCENT * processors);
        return Math.clamp(percent, 0, MAX_PERCENT);
    }
}
