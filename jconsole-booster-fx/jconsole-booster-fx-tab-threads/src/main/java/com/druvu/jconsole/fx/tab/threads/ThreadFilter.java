package com.druvu.jconsole.fx.tab.threads;

import java.util.List;
import java.util.Locale;

/**
 * The thread list's filter: the threads whose name holds the typed text, in any case.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ThreadFilter {

    private ThreadFilter() {}

    /** The threads whose name contains {@code text}, ignoring case, in their order; all of them for an empty text. */
    static List<ThreadEntry> apply(List<ThreadEntry> threads, String text) {
        if (text.isEmpty()) {
            return threads;
        }
        String wanted = text.toLowerCase(Locale.ROOT);
        return threads.stream()
                .filter(thread -> thread.name().toLowerCase(Locale.ROOT).contains(wanted))
                .toList();
    }

    /** The line beside the filter: how many threads the list shows, and of how many when it is filtered. */
    static String countText(int shown, int total) {
        return shown == total ? "Threads: " + total : "Threads: " + shown + " of " + total;
    }
}
