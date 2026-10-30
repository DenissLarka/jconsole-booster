package com.druvu.jconsole.fx.charts;

/**
 * Where the lines are drawn on the canvas, how a sample's time and value map to a pixel there, and where the axes, the
 * plot box and the current values sit around it.
 *
 * @param left the left edge of the lines
 * @param top the top edge of the lines
 * @param width the width the time window spreads over
 * @param height the height the value scale spreads over
 * @param window the time shown
 * @param scale the values shown
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record PlotArea(double left, double top, double width, double height, TimeWindow window, ValueScale scale) {

    /** The plot box reaches this far left and right of the lines. */
    static final double BOX_INSET_X = 5;
    /** The plot box reaches this far above and below the lines. */
    static final double BOX_INSET_Y = 8;
    /** The value axis runs this far left of the lines. */
    static final double VALUE_AXIS_OFFSET = 18;
    /** The value labels end this far left of the value axis. */
    static final double VALUE_LABEL_GAP = 6;
    /** The time axis runs this far below the lines. */
    static final double TIME_AXIS_OFFSET = 15;
    /** The current-value markers start this far right of the plot box. */
    static final double MARKER_GAP = 2;
    /** The current-value labels start this far right of their marker. */
    static final double MARKER_TO_LABEL = 17;
    /** The current-value labels start this far right of the lines. */
    static final double LABEL_OFFSET = BOX_INSET_X + MARKER_GAP + MARKER_TO_LABEL;

    double right() {
        return left + width;
    }

    double bottom() {
        return top + height;
    }

    /** The x of a sample taken at {@code time}. */
    double x(long time) {
        return left + width * (time - window.start()) / window.span();
    }

    /** The y of {@code value}. */
    double y(long value) {
        return bottom() - height * scale.fraction(value);
    }

    double boxLeft() {
        return left - BOX_INSET_X;
    }

    double boxRight() {
        return right() + BOX_INSET_X;
    }

    double boxTop() {
        return top - BOX_INSET_Y;
    }

    double boxBottom() {
        return bottom() + BOX_INSET_Y;
    }

    double valueAxisX() {
        return left - VALUE_AXIS_OFFSET;
    }

    double timeAxisY() {
        return bottom() + TIME_AXIS_OFFSET;
    }

    double markerX() {
        return boxRight() + MARKER_GAP;
    }

    double labelX() {
        return right() + LABEL_OFFSET;
    }
}
