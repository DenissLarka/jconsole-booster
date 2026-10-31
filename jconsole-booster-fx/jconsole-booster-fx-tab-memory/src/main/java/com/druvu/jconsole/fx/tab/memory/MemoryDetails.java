package com.druvu.jconsole.fx.tab.memory;

import com.druvu.jconsole.core.text.Formatter;
import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.text.Resources;
import com.druvu.jconsole.fx.tab.DetailRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The details of the chosen memory area in words, as the Swing JConsole lists them: the time, used, committed, max, the
 * usage threshold, then each garbage collector's time and count.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class MemoryDetails {

    private static final long BYTES_PER_KBYTE = 1024;

    private MemoryDetails() {}

    /** The details of {@code area} at {@code reading}; a value the area does not have is left out. */
    static List<DetailRow> rows(MemoryReading reading, MemoryArea area) {
        List<DetailRow> rows = new ArrayList<>();
        rows.add(new DetailRow(Messages.TIME, Formatter.formatDateTime(reading.timeMillis())));
        Optional<AreaUsage> found = reading.usage(area);
        if (found.isPresent()) {
            AreaUsage usage = found.get();
            rows.add(new DetailRow(Messages.USED, kbytes(usage.used())));
            if (usage.committed() > 0) {
                rows.add(new DetailRow(Messages.COMMITTED, kbytes(usage.committed())));
            }
            if (usage.hasMax()) {
                rows.add(new DetailRow(Messages.MAX, kbytes(usage.max())));
            }
            if (usage.hasThreshold()) {
                rows.add(new DetailRow(Messages.USAGE_THRESHOLD, kbytes(usage.threshold())));
            }
        }
        String name = Messages.GC_TIME;
        for (CollectorTime collector : reading.collectors()) {
            rows.add(new DetailRow(name, collectorText(collector)));
            // the name heads the first collector's line only
            name = "";
        }
        return rows;
    }

    /** A size in whole kilobytes, such as {@code 12,345 kbytes}. */
    static String kbytes(long bytes) {
        return Resources.format(Messages.KBYTES, Formatter.formatLong(bytes / BYTES_PER_KBYTE));
    }

    /** One collector's line, such as {@code 0.250 seconds on G1 Young Generation (12 collections)}. */
    static String collectorText(CollectorTime collector) {
        return Resources.format(
                Messages.GC_TIME_DETAILS,
                Formatter.formatTime(collector.timeMillis()),
                collector.name(),
                Formatter.formatLong(collector.count()));
    }
}
