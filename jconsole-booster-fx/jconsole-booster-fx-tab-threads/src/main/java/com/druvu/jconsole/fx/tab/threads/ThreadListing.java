package com.druvu.jconsole.fx.tab.threads;

import java.util.List;

/**
 * One reading of the target JVM's threads: the counts for the chart and the threads for the list.
 *
 * @param timeMillis when it was read, in epoch milliseconds
 * @param live the live threads
 * @param peak the most live threads at once so far
 * @param threads the live threads, the oldest first
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record ThreadListing(long timeMillis, int live, int peak, List<ThreadEntry> threads) {

    ThreadListing {
        threads = List.copyOf(threads);
    }
}
