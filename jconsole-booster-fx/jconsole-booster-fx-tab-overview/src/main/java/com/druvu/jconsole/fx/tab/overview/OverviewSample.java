package com.druvu.jconsole.fx.tab.overview;

import java.lang.management.MemoryUsage;
import java.util.OptionalDouble;

/**
 * One reading of the values the Overview charts.
 *
 * @param timeMillis when it was read, in epoch milliseconds
 * @param heap the heap's usage
 * @param threads the thread counts
 * @param classes the class counts
 * @param cpuPercent the process's CPU usage since the previous reading; empty for the first reading and when the target
 *     JVM does not measure its CPU time
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record OverviewSample(
        long timeMillis, MemoryUsage heap, ThreadCounts threads, ClassCounts classes, OptionalDouble cpuPercent) {}
