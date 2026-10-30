package com.druvu.jconsole.fx.charts;

import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Paints the lines of the drawn series, the first one on top, and at the right margin a marker with the series name and
 * newest value for each line.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class SeriesPainter {

    static final Font VALUE_FONT = Font.font(10);
    /** The height of one line of a current-value label: the series name, then the value under it. */
    static final double VALUE_LINE_HEIGHT = 12;

    private static final double LINE_WIDTH = 1.5;
    private static final double[] TRANSITION_DASHES = {2, 3};
    /** A label further than this from the end of its line gets a short line pointing at it. */
    private static final double POINTER_THRESHOLD = 3;

    private SeriesPainter() {}

    static void paint(GraphicsContext gc, PlotArea area, SampleStore store, int firstVisible, RenderOptions options) {
        List<Series> drawn = store.series().stream().filter(Series::isPlotted).toList();
        gc.save();
        gc.beginPath();
        gc.rect(area.boxLeft(), area.boxTop(), area.boxRight() - area.boxLeft(), area.boxBottom() - area.boxTop());
        gc.clip();
        // backwards, so that the first series ends up on top
        for (int i = drawn.size() - 1; i >= 0; i--) {
            Series series = drawn.get(i);
            paintLine(gc, area, series, Decimator.points(store, series, firstVisible, area.width()));
        }
        gc.restore();
        if (store.size() > 0) {
            paintCurrentValues(gc, area, drawn, store.size() - 1, options);
        }
    }

    private static void paintLine(GraphicsContext gc, PlotArea area, Series series, List<PlotPoint> points) {
        gc.setStroke(series.color());
        gc.setLineWidth(LINE_WIDTH);
        gc.setLineCap(StrokeLineCap.ROUND);
        PlotPoint previous = null;
        for (PlotPoint point : points) {
            if (previous != null && !previous.isGap() && !point.isGap()) {
                if (series.hasDashedTransitions()) {
                    gc.setLineDashes(previous.value() == point.value() ? null : TRANSITION_DASHES);
                }
                gc.strokeLine(
                        area.x(previous.time()), area.y(previous.value()), area.x(point.time()), area.y(point.value()));
            }
            previous = point;
        }
        gc.setLineDashes((double[]) null);
    }

    private static void paintCurrentValues(
            GraphicsContext gc, PlotArea area, List<Series> drawn, int newest, RenderOptions options) {
        List<Series> shown = drawn.stream()
                .filter(series -> !SampleStore.isGap(series.value(newest)))
                .filter(series -> area.scale().contains(series.value(newest)))
                .toList();
        double[] lineEnds = shown.stream()
                .mapToDouble(series -> area.y(series.value(newest)))
                .toArray();
        double labelHeight = options.showSeriesNames() ? 2 * VALUE_LINE_HEIGHT : VALUE_LINE_HEIGHT;
        double[] labelYs = LabelLayout.spread(lineEnds, labelHeight, area.top(), area.bottom());

        gc.setFont(VALUE_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        gc.setLineWidth(1);
        for (int i = 0; i < shown.size(); i++) {
            Series series = shown.get(i);
            // on a dark plot the text colour reads better than a line colour picked for a light one
            Color colour = options.palette().isLight()
                    ? series.color()
                    : options.palette().text();
            gc.setFill(colour);
            gc.setStroke(colour);
            paintMarker(gc, area, lineEnds[i], labelYs[i]);
            String value = options.format().currentValue(series.value(newest));
            if (options.showSeriesNames()) {
                gc.fillText(series.name(), area.labelX(), labelYs[i] - VALUE_LINE_HEIGHT / 2);
                gc.fillText(value, area.labelX(), labelYs[i] + VALUE_LINE_HEIGHT / 2);
            } else {
                gc.fillText(value, area.labelX(), labelYs[i]);
            }
        }
    }

    /** A small arrow at the end of the line, and a short pointer to its label when the label had to move away. */
    private static void paintMarker(GraphicsContext gc, PlotArea area, double lineEnd, double labelY) {
        double x = area.markerX();
        gc.fillPolygon(new double[] {x, x + 4, x + 4}, new double[] {lineEnd, lineEnd + 3, lineEnd - 3}, 3);
        if (Math.abs(labelY - lineEnd) > POINTER_THRESHOLD) {
            gc.strokeLine(x + 7, lineEnd, x + 12, labelY);
        }
    }
}
