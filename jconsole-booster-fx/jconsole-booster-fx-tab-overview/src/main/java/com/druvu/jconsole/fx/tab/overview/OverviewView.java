package com.druvu.jconsole.fx.tab.overview;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.text.Resources;
import com.druvu.jconsole.fx.charts.ChartUnit;
import com.druvu.jconsole.fx.charts.TimeRangeChooser;
import com.druvu.jconsole.fx.charts.TimeSeriesChart;
import com.druvu.jconsole.fx.charts.TitledChart;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

/**
 * The Overview tab's content: four charts in a two-by-two grid (heap, threads, classes, CPU usage), each with its
 * newest values in words underneath, and one chooser that sets the time range of all four. Built and used on the FX
 * thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class OverviewView {

    static final String HEAP_KEY = "used";
    static final String THREADS_KEY = "threads";
    static final String CLASSES_KEY = "loaded";
    static final String CPU_KEY = "cpu";

    /** The CPU usage is charted with one decimal, so 12.3% is added as 123. */
    private static final int CPU_DECIMALS = 1;

    private static final double CPU_SCALE = 10;
    private static final double GAP = 6;
    private static final double COLUMN_GAP = 10;
    private static final int GRID_SIZE = 2;
    private static final double GRID_SHARE_PERCENT = 50;

    private final TimeSeriesChart heapChart = new TimeSeriesChart(ChartUnit.BYTES);
    private final TimeSeriesChart threadsChart = new TimeSeriesChart();
    private final TimeSeriesChart classesChart = new TimeSeriesChart();
    private final TimeSeriesChart cpuChart = new TimeSeriesChart(ChartUnit.PERCENT, CPU_DECIMALS);
    private final TitledChart heapPane = titled(Messages.HEAP_MEMORY_USAGE, heapChart, HEAP_KEY, Messages.USED);
    private final TitledChart threadsPane = titled(Messages.THREADS, threadsChart, THREADS_KEY, Messages.LIVE_THREADS);
    private final TitledChart classesPane = titled(Messages.CLASSES, classesChart, CLASSES_KEY, Messages.LOADED);
    private final TitledChart cpuPane = titled(Messages.CPU_USAGE, cpuChart, CPU_KEY, Messages.CPU_USAGE);
    private final BorderPane root = new BorderPane();

    OverviewView() {
        TimeRangeChooser chooser = new TimeRangeChooser(heapChart, threadsChart, classesChart, cpuChart);
        Label label = new Label(Messages.TIME_RANGE_COLON);
        label.setLabelFor(chooser);
        HBox controls = new HBox(GAP, label, chooser);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(0, 0, GAP, 0));
        root.setTop(controls);
        root.setCenter(grid());
        root.setPadding(new Insets(GAP));
    }

    Node root() {
        return root;
    }

    /** Puts a reading on screen: a value on each chart and the words under it; the CPU usage only when there is one. */
    void show(OverviewSample sample) {
        long time = sample.timeMillis();
        heapChart.addValues(time, sample.heap().getUsed());
        heapPane.setInfo(InfoLines.heap(sample.heap()));
        threadsChart.addValues(time, sample.threads().live());
        threadsPane.setInfo(InfoLines.threads(sample.threads()));
        classesChart.addValues(time, sample.classes().loaded());
        classesPane.setInfo(InfoLines.classes(sample.classes()));
        sample.cpuPercent().ifPresent(percent -> {
            cpuChart.addValues(time, Math.round(percent * CPU_SCALE));
            cpuPane.setInfo(InfoLines.cpu(percent));
        });
    }

    /** The four charts in two equal columns and two equal rows that share all the room. */
    private GridPane grid() {
        GridPane grid = new GridPane(COLUMN_GAP, GAP);
        grid.add(heapPane, 0, 0);
        grid.add(threadsPane, 1, 0);
        grid.add(classesPane, 0, 1);
        grid.add(cpuPane, 1, 1);
        for (int i = 0; i < GRID_SIZE; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(GRID_SHARE_PERCENT);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(GRID_SHARE_PERCENT);
            row.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(row);
        }
        return grid;
    }

    /** {@code chart} with its one series, under {@code title}. */
    private static TitledChart titled(String title, TimeSeriesChart chart, String key, String seriesName) {
        chart.addSeries(key, seriesName, null, true);
        chart.setAccessibleText(Resources.format(Messages.OVERVIEW_PANEL_PLOTTER_ACCESSIBLE_NAME, title));
        return new TitledChart(title, chart);
    }
}
