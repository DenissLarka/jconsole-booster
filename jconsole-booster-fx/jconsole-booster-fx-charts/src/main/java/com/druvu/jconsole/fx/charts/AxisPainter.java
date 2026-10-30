package com.druvu.jconsole.fx.charts;

import com.druvu.jconsole.core.text.Formatter;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Paints the plot box, the value axis with its labels and horizontal grid lines, and the time axis with its clock
 * labels, dates at midnight, and vertical grid lines.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class AxisPainter {

    static final Font TICK_FONT = Font.font(11);
    /** The height of one line of time labels: the clock time, then the date under it at midnight. */
    static final double TIME_LINE_HEIGHT = 14;
    /** The room the time labels take below the time axis. */
    static final double TIME_LABELS_HEIGHT = 2 * TIME_LINE_HEIGHT + 4;

    private static final double TICK_LENGTH = 2;
    /** The horizontal grid lines stop this far inside the plot box. */
    private static final double GRID_INSET = 4;
    /** A value label closer than this to the one below it is left out. */
    private static final double MIN_LABEL_SPACING = 13;

    private AxisPainter() {}

    static void paint(
            GraphicsContext gc,
            PlotArea area,
            List<String> valueLabels,
            List<TimeTick> timeTicks,
            ChartPalette palette) {
        paintBox(gc, area, palette);
        gc.setFont(TICK_FONT);
        gc.setLineWidth(1);
        gc.setLineDashes((double[]) null);
        paintValueAxis(gc, area, valueLabels, palette);
        paintTimeAxis(gc, area, timeTicks, palette);
    }

    private static void paintBox(GraphicsContext gc, PlotArea area, ChartPalette palette) {
        double width = area.boxRight() - area.boxLeft();
        double height = area.boxBottom() - area.boxTop();
        gc.setFill(palette.plotBackground());
        gc.fillRect(area.boxLeft(), area.boxTop(), width, height);
        gc.setStroke(palette.plotBorder());
        gc.setLineWidth(1);
        gc.strokeRect(crisp(area.boxLeft()), crisp(area.boxTop()), Math.round(width), Math.round(height));
    }

    private static void paintValueAxis(GraphicsContext gc, PlotArea area, List<String> labels, ChartPalette palette) {
        double axisX = crisp(area.valueAxisX());
        gc.setStroke(palette.text());
        gc.strokeLine(axisX, area.top(), axisX, area.bottom());
        gc.setFill(palette.text());
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setTextBaseline(VPos.CENTER);
        List<Long> ticks = area.scale().ticks();
        double lastLabelY = Double.MAX_VALUE;
        for (int i = 0; i < ticks.size(); i++) {
            double y = crisp(area.y(ticks.get(i)));
            gc.setStroke(palette.grid());
            gc.strokeLine(area.boxLeft() + GRID_INSET, y, area.boxRight() - GRID_INSET, y);
            gc.setStroke(palette.text());
            gc.strokeLine(axisX - TICK_LENGTH, y, axisX + TICK_LENGTH, y);
            // the ticks run upwards from the bottom
            if (y < lastLabelY - MIN_LABEL_SPACING) {
                gc.fillText(labels.get(i), axisX - PlotArea.VALUE_LABEL_GAP, y);
                lastLabelY = y;
            }
        }
    }

    private static void paintTimeAxis(GraphicsContext gc, PlotArea area, List<TimeTick> ticks, ChartPalette palette) {
        double axisY = crisp(area.timeAxisY());
        gc.setStroke(palette.text());
        gc.strokeLine(area.left(), axisY, area.right(), axisY);
        gc.setFill(palette.text());
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);
        double clockY = axisY + TICK_LENGTH + 2;
        for (TimeTick tick : ticks) {
            double x = crisp(area.x(tick.time()));
            gc.setStroke(palette.grid());
            gc.strokeLine(x, area.top(), x, area.bottom());
            gc.setStroke(palette.text());
            gc.strokeLine(x, axisY - TICK_LENGTH, x, axisY + TICK_LENGTH);
            gc.fillText(Formatter.formatClockTime(tick.time()), x, clockY);
            if (tick.midnight()) {
                gc.fillText(Formatter.formatDate(tick.time()), x, clockY + TIME_LINE_HEIGHT);
            }
        }
    }

    /** The centre of the pixel holding {@code position}, where a one-pixel line draws sharp. */
    static double crisp(double position) {
        return Math.floor(position) + 0.5;
    }
}
