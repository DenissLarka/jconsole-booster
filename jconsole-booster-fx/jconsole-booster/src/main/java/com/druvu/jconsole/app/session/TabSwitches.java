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
import lombok.extern.slf4j.Slf4j;

/**
 * The user's on/off switch per tab, kept in a hand-editable file of {@code id=on|off} lines; a missing file or id means
 * on. The file is the one the Swing UI keeps its plugin switches in, so every id in it survives a rewrite, the ones
 * this app does not know included. Reading or writing never fails the caller: a broken file reads as all on.
 *
 * <p>FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@Slf4j
public final class TabSwitches {

    private static final List<String> HEADER = List.of(
            "# JConsole Booster plugins - id=on|off (a missing id means on).",
            "# Takes effect for connections opened after the change.");

    private final Path file;

    /** @param file the switches file, created on the first change */
    public TabSwitches(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /** Whether the tab is switched on. */
    public boolean isOn(String id) {
        return !Boolean.FALSE.equals(read().get(id));
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
            log.warn("Could not read {}: {}", file, e.getMessage());
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
            log.warn("Could not write {}: {}", file, e.getMessage());
        }
    }
}
