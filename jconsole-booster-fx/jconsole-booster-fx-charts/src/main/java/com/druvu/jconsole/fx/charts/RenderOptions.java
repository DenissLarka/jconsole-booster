package com.druvu.jconsole.fx.charts;

/**
 * What a chart draws its samples with.
 *
 * @param range the time range shown
 * @param format how the values read
 * @param palette the theme's colours
 * @param showSeriesNames whether each current value shows its series name above it
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record RenderOptions(TimeRange range, ValueFormat format, ChartPalette palette, boolean showSeriesNames) {}
