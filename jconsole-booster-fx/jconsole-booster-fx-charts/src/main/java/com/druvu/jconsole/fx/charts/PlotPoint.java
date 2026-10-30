package com.druvu.jconsole.fx.charts;

/**
 * One point a line is drawn through; a gap point breaks the line.
 *
 * @param time the sample time, in epoch milliseconds
 * @param value the sample value, or {@link SampleStore#GAP}
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record PlotPoint(long time, long value) {

    boolean isGap() {
        return SampleStore.isGap(value);
    }
}
