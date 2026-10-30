package com.druvu.jconsole.core.launch;

import java.util.List;

/**
 * Immutable value class holding all parsed launch options. Produced by {@link ArgumentParser#parse(String[])}.
 *
 * @param colorHex the UI colour as given with {@code -c=}, {@code #RRGGBB} or {@code #RRGGBBAA}; {@code null} when not
 *     given
 */
public record JConsoleOptions(
        boolean noTile,
        boolean hotspot,
        boolean debug,
        int updateInterval,
        String colorHex,
        List<String> urls,
        boolean console,
        List<String> commands,
        String scriptUser) {}
