package com.druvu.jconsole.fx.charts;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;

/**
 * A chart under a title, with an optional line of text under it, such as the newest values in words. It does not
 * collapse unless asked to ({@link #setCollapsible(boolean)}).
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class TitledChart extends TitledPane {

    private final Label info = new Label();

    /** {@code chart} under {@code title}. */
    public TitledChart(String title, TimeSeriesChart chart) {
        setText(title);
        setCollapsible(false);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        // the info line takes room only when it has text
        info.managedProperty().bind(info.textProperty().isNotEmpty());
        info.visibleProperty().bind(info.managedProperty());
        info.setMaxWidth(Double.MAX_VALUE);
        info.setAlignment(Pos.CENTER);
        BorderPane content = new BorderPane(chart);
        content.setBottom(info);
        setContent(content);
    }

    /** Shows {@code text} under the chart; an empty text hides the line. */
    public void setInfo(String text) {
        info.setText(text);
    }

    /** The text under the chart; empty when the line is hidden. */
    public String getInfo() {
        return info.getText();
    }
}
