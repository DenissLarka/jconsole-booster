package com.druvu.jconsole.fx.tab.threads;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads the target JVM's thread counts and live threads, on the update thread. A thread's name is read once, when the
 * thread first shows up, and kept until the thread ends: one round trip for the new threads instead of one per thread
 * on every tick.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ThreadListReader {

    /** Thread names by thread id. */
    private final Map<Long, String> names = new HashMap<>();

    ThreadListing read(ThreadMXBean threads) {
        int live = threads.getThreadCount();
        int peak = threads.getPeakThreadCount();
        long timeMillis = System.currentTimeMillis();
        long[] ids = threads.getAllThreadIds();
        readNewNames(threads, ids);
        Set<Long> alive = Arrays.stream(ids).boxed().collect(Collectors.toSet());
        names.keySet().retainAll(alive);

        List<ThreadEntry> entries = new ArrayList<>(ids.length);
        for (long id : ids) {
            String name = names.get(id);
            if (name != null) {
                entries.add(new ThreadEntry(id, name));
            }
        }
        // a new thread gets a higher id than every thread before it, so this puts the oldest first
        entries.sort(Comparator.comparingLong(ThreadEntry::id));
        return new ThreadListing(timeMillis, live, peak, entries);
    }

    private void readNewNames(ThreadMXBean threads, long[] ids) {
        long[] newIds = Arrays.stream(ids).filter(id -> !names.containsKey(id)).toArray();
        if (newIds.length == 0) {
            return;
        }
        for (ThreadInfo info : threads.getThreadInfo(newIds)) {
            // a thread that ended since getAllThreadIds has no info
            if (info != null) {
                names.put(info.getThreadId(), info.getThreadName());
            }
        }
    }
}
