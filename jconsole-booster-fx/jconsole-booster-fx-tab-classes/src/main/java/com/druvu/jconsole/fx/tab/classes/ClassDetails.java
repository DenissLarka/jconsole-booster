package com.druvu.jconsole.fx.tab.classes;

import com.druvu.jconsole.core.text.Formatter;
import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.DetailRow;
import java.util.List;

/**
 * The class loading details in words, as the Swing JConsole lists them: the time, then the current, total and unloaded
 * class counts.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ClassDetails {

    private ClassDetails() {}

    static List<DetailRow> rows(ClassLoadingReading reading) {
        return List.of(
                new DetailRow(Messages.TIME, Formatter.formatDateTime(reading.timeMillis())),
                new DetailRow(Messages.CURRENT_CLASSES_LOADED, Formatter.formatLong(reading.loaded())),
                new DetailRow(Messages.TOTAL_CLASSES_LOADED, Formatter.formatLong(reading.totalLoaded())),
                new DetailRow(Messages.TOTAL_CLASSES_UNLOADED, Formatter.formatLong(reading.unloaded())));
    }
}
