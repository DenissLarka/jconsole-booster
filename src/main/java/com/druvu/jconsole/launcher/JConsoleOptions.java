package com.druvu.jconsole.launcher;

import java.awt.Color;
import java.util.List;

/** Immutable value class holding all parsed launch options. Produced by {@link ArgumentParser#parse(String[])}. */
public record JConsoleOptions(
        boolean noTile, boolean hotspot, boolean debug, int updateInterval, Color color, List<String> urls) {}
