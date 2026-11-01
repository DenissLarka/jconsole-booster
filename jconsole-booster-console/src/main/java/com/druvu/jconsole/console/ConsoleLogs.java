package com.druvu.jconsole.console;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * The console mode's logging, set up first in {@code main}: the JDK's own backend behind {@link System.Logger}, one
 * line per record on the error stream, druvu's own lines at DEBUG with {@code -debug}. The desktop app has
 * druvu-lib-fx's AppLogs for this; the console runs from a plain jar on JDK 25, without the kit, so it does the same
 * here, minus the log file. A run that configures the backend itself ({@code java.util.logging.config.file} or
 * {@code .config.class}) keeps it.
 *
 * @author Deniss Larka <br>
 *     on 01 Nov 2026
 */
public final class ConsoleLogs {

    private static final String FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";

    // SimpleFormatter's arguments: 1 the time, 3 the logger, 4 the level, 5 the message, 6 the stack trace
    private static final String ONE_LINE = "%1$tH:%1$tM:%1$tS.%1$tL %4$-7s %3$s - %5$s%6$s%n";

    // -debug opens up druvu's own loggers only: at FINE the JDK's would list every trusted CA as TLS starts. Held in a
    // field because the JDK keeps loggers weakly, and a collected logger forgets its level.
    private static final Logger DRUVU = Logger.getLogger("com.druvu");

    private ConsoleLogs() {}

    /** @param debug log at DEBUG rather than INFO */
    public static void setUp(boolean debug) {
        if (System.getProperty("java.util.logging.config.file") != null
                || System.getProperty("java.util.logging.config.class") != null) {
            return;
        }
        // read by each SimpleFormatter when it is created; a format given on the command line wins
        if (System.getProperty(FORMAT_PROPERTY) == null) {
            System.setProperty(FORMAT_PROPERTY, ONE_LINE);
        }
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler); // the JDK's default handler prints two lines per record
        }
        Handler console = new ConsoleHandler();
        console.setFormatter(new SimpleFormatter());
        console.setLevel(debug ? Level.FINE : Level.INFO);
        root.addHandler(console);
        root.setLevel(Level.INFO);
        if (debug) {
            DRUVU.setLevel(Level.FINE);
        }
    }
}
