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

    private static final char BYTE_ORDER_MARK = '﻿';

    private final BufferedReader in;
    private final PrintStream out;
    private final boolean useSystemConsole;
    private boolean firstLine = true;

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
        String line = in.readLine();
        // Windows PowerShell 5.1 starts the text it pipes to a program with a byte-order mark; it is part of no
        // command and no password.
        if (firstLine) {
            firstLine = false;
            if (line != null && !line.isEmpty() && line.charAt(0) == BYTE_ORDER_MARK) {
                return line.substring(1);
            }
        }
        return line;
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
     * Reads a password. Uses {@link Console#readPassword} (no echo) when a terminal is attached; otherwise reads a
     * plain line from {@code in} — piped or scripted stdin, and the tests. A piped password loses its trailing
     * whitespace, so a password that ends in a space has to be typed at a terminal.
     */
    String readPassword(String prompt) throws IOException {
        Console console = useSystemConsole ? System.console() : null;
        // Only a terminal can hide the typing; anything else is read as a line.
        if (console != null && console.isTerminal()) {
            char[] pw = console.readPassword("%s", prompt);
            return pw == null ? null : new String(pw);
        }
        out.print(prompt);
        out.flush();
        String line = readLine();
        // cmd's `echo secret | ...` sends "secret " with the space before the pipe.
        return line == null ? null : line.stripTrailing();
    }
}
