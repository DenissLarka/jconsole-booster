package com.druvu.jconsole.fx.tab.overview;

import com.druvu.jconsole.core.text.Formatter;
import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.text.Resources;
import java.lang.management.MemoryUsage;

/**
 * The lines of text under the Overview's charts, worded as the JDK's JConsole words them.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class InfoLines {

    private InfoLines() {}

    /** Used, committed and max, in byte sizes. */
    static String heap(MemoryUsage heap) {
        return plain(
                Messages.MEMORY_TAB_INFO_LABEL_FORMAT,
                Formatter.formatBytes(heap.getUsed(), false),
                Formatter.formatBytes(heap.getCommitted(), false),
                Formatter.formatBytes(heap.getMax(), false));
    }

    /** Live, peak and total started. */
    static String threads(ThreadCounts threads) {
        return plain(Messages.THREAD_TAB_INFO_LABEL_FORMAT, threads.live(), threads.peak(), threads.totalStarted());
    }

    /** Loaded, unloaded and total loaded. */
    static String classes(ClassCounts classes) {
        return plain(Messages.CLASS_TAB_INFO_LABEL_FORMAT, classes.loaded(), classes.unloaded(), classes.totalLoaded());
    }

    /** The usage with one decimal. */
    static String cpu(double percent) {
        return Resources.format(Messages.CPU_USAGE_FORMAT, String.format("%.1f", percent));
    }

    /** Formats one of the shared messages, written in HTML for Swing labels, as plain text for a JavaFX label. */
    private static String plain(String htmlPattern, Object... arguments) {
        return Resources.format(htmlPattern.replaceAll("<[^>]*>", ""), arguments);
    }
}
