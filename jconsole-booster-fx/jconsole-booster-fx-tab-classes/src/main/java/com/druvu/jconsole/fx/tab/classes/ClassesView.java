package com.druvu.jconsole.fx.tab.classes;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.charts.TimeRangeChooser;
import com.druvu.jconsole.fx.charts.TimeSeriesChart;
import com.druvu.jconsole.fx.charts.TitledChart;
import com.druvu.jconsole.fx.tab.DetailsGrid;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * The Classes page: the time range and the "Verbose Output" switch above the chart of loaded classes, and the details
 * under it. Built and used on the FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ClassesView {

    static final String LOADED_KEY = "loaded";
    static final String TOTAL_LOADED_KEY = "totalLoaded";
    static final String VERBOSE_STYLE_CLASS = "classes-verbose";

    private static final double GAP = 8;

    private final TimeSeriesChart chart = new TimeSeriesChart();
    private final CheckBox verbose = new CheckBox(Messages.VERBOSE_OUTPUT);
    private final DetailsGrid details = new DetailsGrid();
    private final BorderPane root = new BorderPane();

    /** A page whose "Verbose Output" switch, when the user flips it, calls {@code verboseChosen} with its new state. */
    ClassesView(Consumer<Boolean> verboseChosen) {
        chart.addSeries(LOADED_KEY, Messages.LOADED, null, true);
        chart.addSeries(TOTAL_LOADED_KEY, Messages.TOTAL_LOADED, Color.RED, true);

        verbose.getStyleClass().add(VERBOSE_STYLE_CLASS);
        verbose.setTooltip(new Tooltip(Messages.VERBOSE_OUTPUT_TOOLTIP));
        // only a click acts: the state each reading sets fires no action
        verbose.setOnAction(event -> verboseChosen.accept(verbose.isSelected()));

        TimeRangeChooser timeRange = new TimeRangeChooser(chart);
        Label timeRangeLabel = new Label(Messages.TIME_RANGE_COLON);
        timeRangeLabel.setLabelFor(timeRange);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(GAP / 2, timeRangeLabel, timeRange, spacer, verbose);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(4, GAP, 4, GAP));

        TitledPane bottom = new TitledPane(Messages.DETAILS, details);
        bottom.setCollapsible(false);

        root.setTop(controls);
        root.setCenter(new TitledChart(Messages.NUMBER_OF_LOADED_CLASSES, chart));
        root.setBottom(bottom);
    }

    Node root() {
        return root;
    }

    /** Puts a reading on screen. */
    void show(ClassLoadingReading reading) {
        chart.addValues(reading.timeMillis(), reading.loaded(), reading.totalLoaded());
        verbose.setSelected(reading.verbose());
        details.show(ClassDetails.rows(reading));
    }
}
