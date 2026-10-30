package com.druvu.jconsole.fx.charts;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;

/**
 * A drop-down of the time ranges that drives several charts at once. Each chart added takes the chosen range and
 * follows it; a range picked from a chart's own menu moves the chooser, and with it the other charts.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class TimeRangeChooser extends ComboBox<TimeRange> {

    /** A chooser set to {@link TimeRange#DEFAULT}, driving {@code charts}. */
    public TimeRangeChooser(TimeSeriesChart... charts) {
        super(FXCollections.observableArrayList(TimeRange.values()));
        setValue(TimeRange.DEFAULT);
        // every range in view at once
        setVisibleRowCount(TimeRange.values().length);
        for (TimeSeriesChart chart : charts) {
            addChart(chart);
        }
    }

    /** Lets the chooser drive {@code chart}: the chart takes the chosen range now and follows it from then on. */
    public void addChart(TimeSeriesChart chart) {
        chart.writableTimeRange().bindBidirectional(valueProperty());
    }

    /** Lets {@code chart} go its own way again. */
    public void removeChart(TimeSeriesChart chart) {
        chart.writableTimeRange().unbindBidirectional(valueProperty());
    }
}
