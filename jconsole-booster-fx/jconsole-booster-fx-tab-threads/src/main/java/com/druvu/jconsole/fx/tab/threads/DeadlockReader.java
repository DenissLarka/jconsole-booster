package com.druvu.jconsole.fx.tab.threads;

import com.druvu.jconsole.core.vm.JmxDataAccess;
import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the target JVM's deadlocked threads and groups them into cycles, one per deadlock, off the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class DeadlockReader {

    private DeadlockReader() {}

    /** The deadlocks of the target JVM now; none when it has none. */
    static List<DeadlockCycle> read(JmxDataAccess connection) throws IOException {
        long[] ids = connection.findDeadlockedThreads();
        if (ids == null) {
            return List.of();
        }
        return cycles(connection.getThreadMXBean().getThreadInfo(ids, Integer.MAX_VALUE));
    }

    /**
     * Groups deadlocked threads into cycles. A cycle starts at a thread not yet in one and follows each thread to the
     * owner of the lock it waits for, until the chain comes back to a thread already placed or leaves the set.
     */
    static List<DeadlockCycle> cycles(ThreadInfo[] deadlocked) {
        Map<Long, ThreadInfo> byId = new LinkedHashMap<>();
        for (ThreadInfo info : deadlocked) {
            // a thread that ended since the search has no info
            if (info != null) {
                byId.put(info.getThreadId(), info);
            }
        }
        Set<Long> placed = new HashSet<>();
        List<DeadlockCycle> cycles = new ArrayList<>();
        for (ThreadInfo start : byId.values()) {
            List<ThreadEntry> cycle = new ArrayList<>();
            ThreadInfo next = start;
            while (next != null && placed.add(next.getThreadId())) {
                cycle.add(new ThreadEntry(next.getThreadId(), next.getThreadName()));
                next = byId.get(next.getLockOwnerId());
            }
            if (!cycle.isEmpty()) {
                cycles.add(new DeadlockCycle(cycle));
            }
        }
        return cycles;
    }
}
