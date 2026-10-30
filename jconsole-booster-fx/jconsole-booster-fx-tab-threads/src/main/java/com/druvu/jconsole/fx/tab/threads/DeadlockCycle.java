package com.druvu.jconsole.fx.tab.threads;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.text.Resources;
import java.util.List;

/**
 * Deadlocked threads that wait on each other in a ring.
 *
 * @param threads the threads, each waiting for a lock the next one holds
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record DeadlockCycle(List<ThreadEntry> threads) {

    DeadlockCycle {
        threads = List.copyOf(threads);
    }

    /** The title of the tab of the cycle at {@code index}: "Deadlock" when it is the only one, else numbered from 1. */
    static String tabTitle(int index, int cycleCount) {
        return cycleCount == 1 ? Messages.DEADLOCK_TAB : Resources.format(Messages.DEADLOCK_TAB_N, index + 1);
    }
}
