package com.druvu.jconsole.fx.charts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * A chart's colours as CSS properties, so that they follow the app's theme. The chart's own stylesheet maps them to the
 * AtlantaFX theme colours; an app stylesheet may set them on {@code .time-series-chart}. Without a theme they keep
 * light defaults.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ChartColours {

    private static final CssMetaData<TimeSeriesChart, Color> PLOT_BACKGROUND =
            colour("-jcb-chart-plot-background", Color.web("#fafafa"), colours -> colours.plotBackground);
    private static final CssMetaData<TimeSeriesChart, Color> PLOT_BORDER =
            colour("-jcb-chart-plot-border", Color.web("#a0a0a0"), colours -> colours.plotBorder);
    private static final CssMetaData<TimeSeriesChart, Color> TEXT =
            colour("-jcb-chart-text", Color.web("#1f2328"), colours -> colours.text);
    private static final CssMetaData<TimeSeriesChart, Color> GRID =
            colour("-jcb-chart-grid", Color.web("#d0d0d0"), colours -> colours.grid);

    /** The CSS properties of a chart: a region's, and the four colours. */
    static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA = cssMetaData();

    private final StyleableObjectProperty<Color> plotBackground;
    private final StyleableObjectProperty<Color> plotBorder;
    private final StyleableObjectProperty<Color> text;
    private final StyleableObjectProperty<Color> grid;

    /** The colours of {@code chart}; {@code onChange} runs when the theme changes one of them. */
    ChartColours(TimeSeriesChart chart, Runnable onChange) {
        plotBackground = property(PLOT_BACKGROUND, chart, "plotBackground", onChange);
        plotBorder = property(PLOT_BORDER, chart, "plotBorder", onChange);
        text = property(TEXT, chart, "text", onChange);
        grid = property(GRID, chart, "grid", onChange);
    }

    ChartPalette palette() {
        return new ChartPalette(plotBackground.get(), plotBorder.get(), text.get(), grid.get());
    }

    private static StyleableObjectProperty<Color> property(
            CssMetaData<TimeSeriesChart, Color> metaData, TimeSeriesChart chart, String name, Runnable onChange) {
        StyleableObjectProperty<Color> property =
                new SimpleStyleableObjectProperty<>(metaData, chart, name, metaData.getInitialValue(chart));
        property.addListener((observable, previous, current) -> onChange.run());
        return property;
    }

    private static CssMetaData<TimeSeriesChart, Color> colour(
            String cssName, Color initial, Function<ChartColours, StyleableObjectProperty<Color>> field) {
        return new CssMetaData<>(cssName, StyleConverter.getColorConverter(), initial) {
            @Override
            public boolean isSettable(TimeSeriesChart chart) {
                return !field.apply(chart.colours()).isBound();
            }

            @Override
            public StyleableProperty<Color> getStyleableProperty(TimeSeriesChart chart) {
                return field.apply(chart.colours());
            }
        };
    }

    private static List<CssMetaData<? extends Styleable, ?>> cssMetaData() {
        List<CssMetaData<? extends Styleable, ?>> all = new ArrayList<>(Region.getClassCssMetaData());
        all.addAll(List.of(PLOT_BACKGROUND, PLOT_BORDER, TEXT, GRID));
        return List.copyOf(all);
    }
}
