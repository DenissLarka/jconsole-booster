package com.druvu.jconsole.fx.tab.jtop;

import java.util.List;

/**
 * One reading of the target JVM's threads, ready for the screen.
 *
 * @param rows the threads, the busiest first
 * @param status the line under the table: the thread count, or why there are no rows
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record ThreadSnapshot(List<ThreadRow> rows, String status) {

    ThreadSnapshot {
        rows = List.copyOf(rows);
    }

    /** A reading that could not list the threads, and says why. */
    static ThreadSnapshot withoutRows(String reason) {
        return new ThreadSnapshot(List.of(), reason);
    }
}
