package com.druvu.jconsole.fx.charts;

import javafx.scene.paint.Color;

/**
 * The colours a chart draws with besides its lines, as the theme resolved them.
 *
 * @param plotBackground the fill of the plot box
 * @param plotBorder the outline of the plot box
 * @param text the axes and their labels
 * @param grid the grid lines
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record ChartPalette(Color plotBackground, Color plotBorder, Color text, Color grid) {

    /** Whether the plot is light, so that the labels may take their lines' colours and still read well. */
    boolean isLight() {
        return plotBackground.getBrightness() > 0.5;
    }
}
