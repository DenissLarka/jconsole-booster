package com.druvu.jconsole.fx.charts;

/**
 * What the values of a chart measure, which decides how its axis and its current values read.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public enum ChartUnit {
    /** Plain numbers. */
    NONE,
    /** Byte sizes: the axis reads in Kb, Mb or Gb, and the first series alone sets the scale. */
    BYTES,
    /** Percentages: the axis ticks and the current values end in {@code %}. */
    PERCENT
}
