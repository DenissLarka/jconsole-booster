package com.druvu.jconsole.app;

import com.druvu.jconsole.console.ConsoleMain;
import com.druvu.jconsole.core.launch.ArgumentParser;
import com.druvu.jconsole.core.launch.JConsoleOptions;
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

    private static final String SIMPLE_LOGGER_LEVEL = "org.slf4j.simpleLogger.defaultLogLevel";

    private Main() {}

    public static void main(String[] args) {
        // -debug turns on debug logging; it has to be set before the first logger exists, i.e. before parsing.
        if (Arrays.asList(args).contains("-debug") && System.getProperty(SIMPLE_LOGGER_LEVEL) == null) {
            System.setProperty(SIMPLE_LOGGER_LEVEL, "debug");
        }
        Optional<JConsoleOptions> options = ArgumentParser.parse(args);
        if (options.isEmpty()) {
            // the usage or the version was printed
            return;
        }
        if (options.get().console()) {
            System.exit(ConsoleMain.run(options.get()));
        }
        JConsoleBoosterApp.launchWith(args);
    }
}
