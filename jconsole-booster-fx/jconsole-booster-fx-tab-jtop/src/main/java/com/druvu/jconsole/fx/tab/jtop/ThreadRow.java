package com.druvu.jconsole.fx.tab.jtop;

import java.util.Comparator;

/**
 * One line of the JTop table: a live thread of the target JVM and the CPU time it has used.
 *
 * @param name the thread's name
 * @param cpuSeconds the CPU time the thread has used since it started, in seconds
 * @param state the thread's state when it was read
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record ThreadRow(String name, double cpuSeconds, Thread.State state) {

    /** The busiest thread first; equal times go by name, so idle threads keep their place between readings. */
    static final Comparator<ThreadRow> BUSIEST_FIRST =
            Comparator.comparingDouble(ThreadRow::cpuSeconds).reversed().thenComparing(ThreadRow::name);
}
