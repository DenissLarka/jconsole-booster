package com.druvu.jconsole.fx.tab.summary;

import com.druvu.jconsole.core.text.Formatter;
import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.text.Resources;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns the numbers and lists read from the target JVM into the texts of the summary page. A negative number is how the
 * platform MXBeans say "not available", so it reads as such instead of as {@code -1}.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class SummaryFormat {

    private SummaryFormat() {}

    /** A count with thousands separators. */
    static String count(long count) {
        return count < 0 ? Messages.UNAVAILABLE : Formatter.formatLong(count);
    }

    /** A size in bytes, shown in kilobytes as JConsole does. */
    static String kbytes(long bytes) {
        return bytes < 0 ? Messages.UNAVAILABLE : Resources.format(Messages.KBYTES, Formatter.formatLong(bytes / 1024));
    }

    /** A duration in milliseconds, e.g. {@code 2 hours 5 minutes}. */
    static String millis(long millis) {
        return millis < 0 ? Messages.UNAVAILABLE : Formatter.formatTime(millis);
    }

    /** A duration in nanoseconds, shown as {@link #millis}. */
    static String nanos(long nanos) {
        return nanos < 0 ? Messages.UNAVAILABLE : millis(nanos / 1_000_000);
    }

    /** A collector's work: how many collections and how long they took in total. */
    static String collections(long count, long millis) {
        if (count < 0) {
            return Messages.UNAVAILABLE;
        }
        String noun = count == 1 ? " collection" : " collections";
        return Formatter.formatLong(count) + noun + ", " + millis(millis);
    }

    /**
     * A path, one entry per line, so a long class path reads as a list. The separator is the target's, not this JVM's:
     * the target may run on another operating system.
     */
    static String pathEntries(String path, String separator) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return Arrays.stream(path.split(Pattern.quote(separator)))
                .filter(entry -> !entry.isBlank())
                .collect(Collectors.joining("\n"));
    }

    /** The VM's arguments, one per line. */
    static String arguments(List<String> arguments) {
        return String.join("\n", arguments);
    }

    /** The path separator of an operating system known by its name. */
    static String pathSeparatorOf(String osName) {
        return osName != null && osName.startsWith("Windows") ? ";" : ":";
    }
}
