package com.druvu.jconsole.fx.tab.memory;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.charts.TimeRangeChooser;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * The Memory page: the chart chooser, the time range and the "Perform GC" button above the chosen area's chart, and
 * under it the details beside the pools' usage bars.
 *
 * <p>Every area has its own chart, fed at every reading, so each keeps its whole history while another is shown. An
 * area gets its chart and its chooser entry at the first reading that has it. Built and used on the FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class MemoryView {

    static final String CHOOSER_STYLE_CLASS = "memory-chart-chooser";
    static final String GC_BUTTON_STYLE_CLASS = "memory-gc-button";

    private static final double GAP = 8;

    private final ComboBox<MemoryArea> chooser = new ComboBox<>();
    private final TimeRangeChooser timeRange = new TimeRangeChooser();
    private final StackPane chartPane = new StackPane();
    private final DetailsGrid details = new DetailsGrid();
    private final PoolBars poolBars = new PoolBars(chooser::setValue);
    private final Map<MemoryArea, UsageChart> charts = new HashMap<>();
    private final BorderPane root = new BorderPane();

    /** The newest reading, whose details an area chosen between readings shows. */
    private MemoryReading latest;

    /** A page whose "Perform GC" button runs {@code performGc}. */
    MemoryView(Runnable performGc) {
        chooser.getStyleClass().add(CHOOSER_STYLE_CLASS);
        chooser.valueProperty().addListener((observable, previous, area) -> showArea(area));

        Button gc = new Button(Messages.PERFORM_GC);
        gc.getStyleClass().add(GC_BUTTON_STYLE_CLASS);
        gc.setTooltip(new Tooltip(Messages.PERFORM_GC_TOOLTIP));
        gc.setOnAction(event -> performGc.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(
                GAP * 2,
                labelled(Messages.CHART_COLON, chooser),
                labelled(Messages.TIME_RANGE_COLON, timeRange),
                spacer,
                gc);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(4, GAP, 4, GAP));

        HBox.setHgrow(details, Priority.ALWAYS);
        TitledPane bottom = new TitledPane(Messages.DETAILS, new HBox(GAP, details, poolBars.root()));
        bottom.setCollapsible(false);

        root.setTop(controls);
        root.setCenter(chartPane);
        root.setBottom(bottom);
    }

    Node root() {
        return root;
    }

    /** Puts a reading on screen. */
    void show(MemoryReading reading) {
        latest = reading;
        for (AreaUsage usage : reading.areas()) {
            chart(usage.area()).add(reading.timeMillis(), usage);
        }
        poolBars.show(reading.pools());
        if (chooser.getValue() == null) {
            // the first reading: open on the heap, the first entry
            chooser.getSelectionModel().selectFirst();
        }
        showDetails();
    }

    private UsageChart chart(MemoryArea area) {
        UsageChart chart = charts.get(area);
        if (chart == null) {
            chart = new UsageChart(area);
            charts.put(area, chart);
            timeRange.addChart(chart.chart());
            chooser.getItems().add(area);
        }
        return chart;
    }

    private void showArea(MemoryArea area) {
        if (area == null) {
            return;
        }
        chartPane.getChildren().setAll(charts.get(area).chart());
        poolBars.showSelected(area);
        showDetails();
    }

    private void showDetails() {
        MemoryArea area = chooser.getValue();
        if (latest != null && area != null) {
            details.show(MemoryDetails.rows(latest, area));
        }
    }

    private static HBox labelled(String text, Control control) {
        Label label = new Label(text);
        label.setLabelFor(control);
        HBox labelled = new HBox(GAP / 2, label, control);
        labelled.setAlignment(Pos.CENTER_LEFT);
        return labelled;
    }
}
