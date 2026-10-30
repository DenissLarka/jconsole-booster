package com.druvu.jconsole.fx.tab.threads;

import com.druvu.jconsole.core.vm.JmxDataAccess;
import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Reads one thread's details for the thread lists, off the FX thread. Where the target JVM reports monitor usage, the
 * stack comes with the monitors each frame holds.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class StackReader {

    /** The details of a thread that ended before they were read. */
    static final String THREAD_ENDED = "[The thread has ended]";

    /** The live connection; reading through it is this class's job. */
    private final JmxDataAccess connection;

    StackReader(JmxDataAccess connection) {
        this.connection = connection;
    }

    /** The details of the thread {@code threadId}, in words. */
    String read(long threadId) throws IOException {
        ThreadMXBean threads = connection.getThreadMXBean();
        ThreadInfo info = monitorsReported(threads)
                // this one thread with its locked monitors, rather than a dump of every thread
                ? threads.getThreadInfo(new long[] {threadId}, true, false)[0]
                : threads.getThreadInfo(threadId, Integer.MAX_VALUE);
        return info == null ? THREAD_ENDED : StackText.of(info);
    }

    private boolean monitorsReported(ThreadMXBean threads) {
        return connection.isLockUsageSupported() && threads.isObjectMonitorUsageSupported();
    }
}
