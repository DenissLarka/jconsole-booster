package com.druvu.jconsole.console;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Minimal console IO abstraction so the REPL can be driven by scripted input in tests: it wraps a
 * {@link BufferedReader} for input and a {@link PrintStream} for output, and reads passwords without echo when a real
 * console is attached.
 */
final class ConsoleIO {

    private final BufferedReader in;
    private final PrintStream out;
    private final boolean useSystemConsole;

    /** Test/piped constructor: passwords are read from {@code in} (no {@link System#console()} — deterministic). */
    ConsoleIO(BufferedReader in, PrintStream out) {
        this(in, out, false);
    }

    ConsoleIO(BufferedReader in, PrintStream out, boolean useSystemConsole) {
        this.in = in;
        this.out = out;
        this.useSystemConsole = useSystemConsole;
    }

    String readLine() throws IOException {
        return in.readLine();
    }

    void print(String s) {
        out.print(s);
    }

    void println(String s) {
        out.println(s);
    }

    void println() {
        out.println();
    }

    PrintStream out() {
        return out;
    }

    /**
     * Reads a password. Uses {@link Console#readPassword} (no echo) when a real console is attached; otherwise falls
     * back to a plain line read from {@code in} — required for piped or scripted (test) stdin, where no console exists.
     */
    String readPassword(String prompt) throws IOException {
        Console console = useSystemConsole ? System.console() : null;
        if (console != null) {
            char[] pw = console.readPassword("%s", prompt);
            return pw == null ? null : new String(pw);
        }
        out.print(prompt);
        out.flush();
        return in.readLine();
    }
}
