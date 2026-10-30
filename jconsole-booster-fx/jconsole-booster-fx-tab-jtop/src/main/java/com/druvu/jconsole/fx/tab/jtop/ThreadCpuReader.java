package com.druvu.jconsole.fx.tab.jtop;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the target JVM's threads and the CPU time each has used, the busiest first. Every call on the bean is a JMX
 * round trip, so this runs on the update thread, never on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ThreadCpuReader {

    static final String NO_THREAD_BEAN = "The target JVM has no thread MXBean.";
    static final String CPU_TIME_UNSUPPORTED = "The target JVM does not measure thread CPU time.";
    static final String CPU_TIME_REFUSED = "The target JVM refused to turn on thread CPU time measurement.";

    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private ThreadCpuReader() {}

    /**
     * Reads every live thread of the target JVM.
     *
     * @param threads the target JVM's thread MXBean, null when it has none
     * @return the threads the busiest first, or no rows and the reason in the status
     */
    static ThreadSnapshot read(ThreadMXBean threads) {
        if (threads == null) {
            return ThreadSnapshot.withoutRows(NO_THREAD_BEAN);
        }
        if (!threads.isThreadCpuTimeSupported()) {
            return ThreadSnapshot.withoutRows(CPU_TIME_UNSUPPORTED);
        }
        if (!threads.isThreadCpuTimeEnabled() && !turnOnCpuTime(threads)) {
            return ThreadSnapshot.withoutRows(CPU_TIME_REFUSED);
        }
        List<ThreadRow> rows = rows(threads);
        return new ThreadSnapshot(rows, "Threads: " + rows.size());
    }

    /** Some JVMs start with the measurement off; once on it stays on, so in practice this runs once per target. */
    private static boolean turnOnCpuTime(ThreadMXBean threads) {
        try {
            threads.setThreadCpuTimeEnabled(true);
            return true;
        } catch (SecurityException refused) {
            return false;
        }
    }

    private static List<ThreadRow> rows(ThreadMXBean threads) {
        long[] ids = threads.getAllThreadIds();
        ThreadInfo[] infos = threads.getThreadInfo(ids);
        List<ThreadRow> rows = new ArrayList<>(ids.length);
        for (int i = 0; i < ids.length; i++) {
            // A thread that ended since getAllThreadIds has no info and a CPU time of -1.
            ThreadInfo info = infos[i];
            if (info != null) {
                long cpuNanos = threads.getThreadCpuTime(ids[i]);
                if (cpuNanos >= 0) {
                    rows.add(new ThreadRow(info.getThreadName(), cpuNanos / NANOS_PER_SECOND, info.getThreadState()));
                }
            }
        }
        rows.sort(ThreadRow.BUSIEST_FIRST);
        return rows;
    }
}
