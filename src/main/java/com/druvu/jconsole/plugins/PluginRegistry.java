/*
 * Copyright (c) 2026 JConsole Booster contributors.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

package com.druvu.jconsole.plugins;

import com.druvu.jconsole.plugins.jtop.JTopPlugin;
import com.druvu.jconsole.util.BoosterHome;
import com.sun.tools.jconsole.JConsolePlugin;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry of the plugins bundled into JConsole Booster, with a persisted per-plugin on/off switch.
 *
 * <p>To bundle a new plugin, drop its source under {@code com.druvu.jconsole.plugins.<name>} and add a descriptor to
 * the bundled list below. Plugins are compiled into the app — this is not a discovery mechanism, and the package is
 * deliberately not exported from the module.
 *
 * <p>Enabled-state lives in {@link BoosterHome#pluginsFile()} ({@code id=on|off}, one per line, hand-editable; a
 * missing file or id means on). A disabled plugin is never instantiated: no tab, no update worker. Toggles take effect
 * for connections opened after the change. All I/O is best-effort: a read/write failure is logged at WARN and treated
 * as all-enabled rather than propagated.
 */
@Slf4j
public final class PluginRegistry {

    /** A bundled plugin: stable id (persisted in the on/off file), menu display name, instance factory. */
    public record PluginDescriptor(String id, String displayName, Supplier<JConsolePlugin> factory) {}

    /**
     * Feature toggle for the classic JConsole tabs (Overview, Memory, Threads, Classes, VM Summary) as one group — they
     * toggle together because Overview aggregates the other tabs' plotters. Not a {@link JConsolePlugin}; the switch is
     * consulted by {@code VMPanel} during tab assembly. MBeans is deliberately not toggleable.
     */
    public static final String DEFAULT_TABS_ID = "default-tabs";

    private static final List<PluginDescriptor> BUNDLED =
            List.of(new PluginDescriptor("jtop", "JTop", JTopPlugin::new));

    private PluginRegistry() {}

    public static List<PluginDescriptor> bundled() {
        return BUNDLED;
    }

    public static synchronized boolean isEnabled(String id) {
        return !loadDisabled(BoosterHome.pluginsFile()).contains(id);
    }

    public static synchronized void setEnabled(String id, boolean enabled) {
        Path file = BoosterHome.pluginsFile();
        Set<String> disabled = loadDisabled(file);
        boolean changed = enabled ? disabled.remove(id) : disabled.add(id);
        if (changed) {
            store(file, disabled);
        }
    }

    /** Fresh instances of every enabled bundled plugin; disabled plugins are never instantiated. */
    public static synchronized List<JConsolePlugin> newEnabledInstances() {
        return instantiate(BUNDLED, loadDisabled(BoosterHome.pluginsFile()));
    }

    static List<JConsolePlugin> instantiate(List<PluginDescriptor> descriptors, Set<String> disabled) {
        List<JConsolePlugin> out = new ArrayList<>();
        for (PluginDescriptor d : descriptors) {
            if (!disabled.contains(d.id())) {
                out.add(d.factory().get());
            }
        }
        return out;
    }

    /** Returns the ids switched off in {@code file}; ids not in the bundled list are kept, never dropped. */
    static Set<String> loadDisabled(Path file) {
        Set<String> disabled = new LinkedHashSet<>();
        if (!Files.exists(file)) {
            return disabled;
        }
        try {
            for (String raw : Files.readAllLines(file)) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0 && line.substring(eq + 1).strip().equalsIgnoreCase("off")) {
                    disabled.add(line.substring(0, eq).strip());
                }
            }
        } catch (IOException e) {
            log.warn("Could not read {}: {}", file, e.getMessage());
        }
        return disabled;
    }

    static void store(Path file, Set<String> disabled) {
        List<String> known = new ArrayList<>();
        known.add(DEFAULT_TABS_ID);
        for (PluginDescriptor d : BUNDLED) {
            known.add(d.id());
        }
        List<String> lines = new ArrayList<>();
        lines.add("# JConsole Booster plugins - id=on|off (a missing id means on).");
        lines.add("# Takes effect for connections opened after the change.");
        for (String id : known) {
            lines.add(id + "=" + (disabled.contains(id) ? "off" : "on"));
        }
        for (String id : disabled) {
            if (!known.contains(id)) {
                lines.add(id + "=off");
            }
        }
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            log.warn("Could not write {}: {}", file, e.getMessage());
        }
    }
}
