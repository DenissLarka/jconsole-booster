package com.druvu.jconsole.fx.charts;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;

/**
 * Draws a whole chart on its canvas: it works out the window, the scale and margins wide enough for the labels, then
 * has the axes and the lines painted.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ChartRenderer {

    /** The room kept clear around the drawing. */
    private static final double PADDING = 4;
    /** Below this many pixels a plot is too small to draw. */
    private static final double MIN_PLOT_SIZE = 10;

    private final SampleStore store;
    private final TextMeasure measure = new TextMeasure();

    ChartRenderer(SampleStore store) {
        this.store = store;
    }

    void render(GraphicsContext gc, double width, double height, RenderOptions options) {
        gc.clearRect(0, 0, width, height);
        TimeWindow window = TimeWindow.of(options.range(), store, System.currentTimeMillis());
        int firstVisible = store.firstIndexAtOrAfter(window.start());
        ValueScale scale =
                ValueScale.forVisible(store, firstVisible, options.format().unit());
        List<String> valueLabels = options.format().tickLabels(scale.ticks(), scale.max());
        PlotArea area = layOut(width, height, window, scale, valueLabels, options);
        if (area.width() < MIN_PLOT_SIZE || area.height() < MIN_PLOT_SIZE) {
            return;
        }
        List<TimeTick> timeTicks = TimeAxis.ticks(window, area.width(), ZoneId.systemDefault());
        AxisPainter.paint(gc, area, valueLabels, timeTicks, options.palette());
        SeriesPainter.paint(gc, area, store, firstVisible, options);
    }

    private PlotArea layOut(
            double width,
            double height,
            TimeWindow window,
            ValueScale scale,
            List<String> valueLabels,
            RenderOptions options) {
        double left = PADDING
                + measure.widest(valueLabels, AxisPainter.TICK_FONT)
                + PlotArea.VALUE_LABEL_GAP
                + PlotArea.VALUE_AXIS_OFFSET;
        double right = PlotArea.LABEL_OFFSET + widestCurrentValueLabel(scale, options) + PADDING;
        double top = PlotArea.BOX_INSET_Y + PADDING;
        double bottom = PlotArea.TIME_AXIS_OFFSET + AxisPainter.TIME_LABELS_HEIGHT + PADDING;
        return new PlotArea(left, top, width - left - right, height - top - bottom, window, scale);
    }

    /**
     * The widest label the right margin holds: a series name, or a current value, which never reaches past the scale.
     * Sizing the margin by the scale rather than the values keeps the plot still while the values move.
     */
    private double widestCurrentValueLabel(ValueScale scale, RenderOptions options) {
        List<String> labels = new ArrayList<>();
        labels.add(options.format().currentValue(scale.max()));
        labels.add(options.format().currentValue(scale.min()));
        if (options.showSeriesNames()) {
            store.series().stream().filter(Series::isPlotted).map(Series::name).forEach(labels::add);
        }
        return measure.widest(labels, SeriesPainter.VALUE_FONT);
    }
}
