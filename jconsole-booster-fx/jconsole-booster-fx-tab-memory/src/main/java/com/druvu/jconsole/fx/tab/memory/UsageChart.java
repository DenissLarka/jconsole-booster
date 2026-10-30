package com.druvu.jconsole.fx.tab.memory;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.charts.ChartUnit;
import com.druvu.jconsole.fx.charts.TimeSeriesChart;
import javafx.scene.paint.Color;

/**
 * The chart of one memory area: used bytes drawn, committed and max kept for the CSV export, and for a pool its usage
 * threshold as a red dashed line once it has one. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class UsageChart {

    static final String USED_KEY = "used";
    static final String COMMITTED_KEY = "committed";
    static final String MAX_KEY = "max";
    static final String THRESHOLD_KEY = "threshold";

    private final TimeSeriesChart chart = new TimeSeriesChart(ChartUnit.BYTES);
    private final boolean pool;

    UsageChart(MemoryArea area) {
        this.pool = area.isPool();
        chart.addSeries(USED_KEY, Messages.USED, null, true);
        chart.addSeries(COMMITTED_KEY, Messages.COMMITTED, null, false);
        chart.addSeries(MAX_KEY, Messages.MAX, null, false);
        if (pool) {
            chart.addSeries(THRESHOLD_KEY, Messages.THRESHOLD, Color.RED, false);
            chart.setDashedTransitions(THRESHOLD_KEY, true);
        }
    }

    TimeSeriesChart chart() {
        return chart;
    }

    /** Adds the usage read at {@code timeMillis}. */
    void add(long timeMillis, AreaUsage usage) {
        if (!pool) {
            chart.addValues(timeMillis, usage.used(), usage.committed(), usage.max());
            return;
        }
        chart.addValues(timeMillis, usage.used(), usage.committed(), usage.max(), usage.threshold());
        if (usage.hasThreshold()) {
            // once shown the line stays, so its history stays in view if the threshold is turned off
            chart.setPlotted(THRESHOLD_KEY, true);
        }
    }
}
