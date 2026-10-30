package com.druvu.jconsole.launcher;

import com.druvu.jconsole.console.ConsoleMain;

/**
 * Thin launcher entry point that routes between the Swing GUI ({@link JConsole}) and the interactive command-line mode
 * ({@link ConsoleMain}) based on the {@code --console} flag.
 *
 * <p>Deliberately has no Swing supertype. {@code JConsole extends JFrame}, so merely referencing {@code JConsole} runs
 * the {@code Component} static-init chain (native {@code libawt} load, EDT bootstrap) before {@code main}'s first
 * statement. In console mode {@code Main} never touches the {@code JConsole} class, keeping the console path fully
 * headless-capable.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        ArgumentParser.parse(args).ifPresent(options -> {
            if (options.console()) {
                ConsoleMain.run(options);
            } else {
                // JConsole.main re-parses args (harmless) — keeps JConsole directly launchable as today.
                JConsole.main(args);
            }
        });
    }
}
