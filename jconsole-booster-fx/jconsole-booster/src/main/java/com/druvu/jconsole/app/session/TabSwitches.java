package com.druvu.jconsole.app.session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The user's on/off switch per tab, kept in a hand-editable file of {@code id=on|off} lines; a missing file or id means
 * on. The file is the one 1.1 kept its plugin switches in, so every id in it survives a rewrite, the ones this app does
 * not know included. 1.1 switched its five classic tabs as one group, {@code default-tabs}: a classic tab without a
 * line of its own still follows that group. Reading or writing never fails the caller: a broken file reads as all on.
 *
 * <p>FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class TabSwitches {

    private static final System.Logger LOG = System.getLogger(TabSwitches.class.getName());

    private static final List<String> HEADER = List.of(
            "# JConsole Booster plugins - id=on|off (a missing id means on).",
            "# Takes effect for connections opened after the change.");

    // 1.1's one switch for Overview, Memory, Threads, Classes and VM Summary
    private static final String CLASSIC_GROUP = "default-tabs";
    private static final Set<String> CLASSIC_TABS = Set.of("overview", "memory", "threads", "classes", "summary");

    private final Path file;

    /** @param file the switches file, created on the first change */
    public TabSwitches(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /** Whether the tab is switched on: its own line, else for a classic tab 1.1's group line, else on. */
    public boolean isOn(String id) {
        Map<String, Boolean> switches = read();
        Boolean on = switches.get(id);
        if (on == null && CLASSIC_TABS.contains(id)) {
            on = switches.get(CLASSIC_GROUP);
        }
        return !Boolean.FALSE.equals(on);
    }

    /** Switches a tab on or off, from the next connect on. */
    public void set(String id, boolean on) {
        Map<String, Boolean> switches = read();
        switches.put(id, on);
        write(switches);
    }

    private Map<String, Boolean> read() {
        Map<String, Boolean> switches = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return switches;
        }
        try {
            for (String raw : Files.readAllLines(file)) {
                String line = raw.strip();
                int equals = line.indexOf('=');
                if (line.startsWith("#") || equals <= 0) {
                    continue;
                }
                String value = line.substring(equals + 1).strip().toLowerCase(Locale.ROOT);
                switches.put(line.substring(0, equals).strip(), !"off".equals(value));
            }
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not read " + file + ": " + e.getMessage());
        }
        return switches;
    }

    private void write(Map<String, Boolean> switches) {
        List<String> lines = new ArrayList<>(HEADER);
        switches.forEach((id, on) -> lines.add(id + "=" + (on ? "on" : "off")));
        try {
            Path directory = file.toAbsolutePath().getParent();
            if (directory != null) {
                Files.createDirectories(directory);
            }
            Files.write(file, lines);
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not write " + file + ": " + e.getMessage());
        }
    }
}
