package com.druvu.jconsole;

import java.awt.Color;
import java.util.List;

/**
 * Immutable value class holding all parsed launch options. Produced by
 * {@link ArgumentParser#parse(String[])}.
 */
public record JConsoleOptions(
        boolean noTile,
        boolean hotspot,
        boolean debug,
        int updateInterval,
        String pluginPath,
        Color color,
        List<String> urls,
        List<LocalVirtualMachine> vmids) {}
