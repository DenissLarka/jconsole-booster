package com.druvu.jconsole.app;

import com.druvu.jconsole.console.ConsoleLogs;
import com.druvu.jconsole.console.ConsoleMain;
import com.druvu.jconsole.core.launch.ArgumentParser;
import com.druvu.jconsole.core.launch.JConsoleOptions;
import com.druvu.lib.fx.os.AppLogs;
import java.util.Arrays;
import java.util.Optional;

/**
 * Starts JConsole Booster: the console mode when {@code --console} or {@code -e=} asks for it, the JavaFX app
 * otherwise. Nothing of JavaFX is touched before that choice, so the console mode runs on a machine without a display.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        boolean debug = Arrays.asList(args).contains(ArgumentParser.DEBUG_FLAG);
        // One line per record from the first message on, the parsing's own included. The console mode keeps it.
        ConsoleLogs.setUp(debug);
        Optional<JConsoleOptions> options = ArgumentParser.parse(args);
        if (options.isEmpty()) {
            // the usage or the version was printed
            return;
        }
        if (options.get().console()) {
            System.exit(ConsoleMain.run(options.get()));
        }
        // The desktop app logs through the kit: its format, the level from -Ddruvu.log (-debug sets it) and, for an
        // installed copy, the output in ~/.druvu.com/jconsole-booster/logs. Not in console mode: an installed copy
        // would send the console's own output into that file.
        if (debug && System.getProperty(AppLogs.LEVEL_PROPERTY) == null) {
            System.setProperty(AppLogs.LEVEL_PROPERTY, System.Logger.Level.DEBUG.name());
        }
        AppLogs.setUp(JConsoleBoosterApp.HOME);
        JConsoleBoosterApp.launchWith(args);
    }
}
