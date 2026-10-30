package com.druvu.jconsole.fx.charts;

import com.druvu.jconsole.core.text.Messages;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalLong;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * A live time-series chart drawn on a canvas: named series of values against time, a value axis in plain numbers, byte
 * sizes or percentages, a clock time axis, and the newest value of each line at the right margin. Its context menu
 * picks the time range and saves the data as CSV.
 *
 * <p>Values are longs. With {@code decimals} above zero a value is added shifted left by that many decimal places: 1.5
 * with one decimal is added as 15. Add every series before the first values; each sample then carries one value per
 * series, in the order the series were added. The first series is the main one: it is drawn on top and, for byte sizes,
 * alone sets the scale.
 *
 * <p>Colours follow the app's theme through CSS: the chart has the style class {@code time-series-chart} and the colour
 * properties {@code -jcb-chart-plot-background}, {@code -jcb-chart-plot-border}, {@code -jcb-chart-text} and
 * {@code -jcb-chart-grid}, which its own stylesheet maps to the AtlantaFX theme colours. Line colours are the series'
 * own.
 *
 * <p>Threads: call every method on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class TimeSeriesChart extends Region {

    /** The colour of a series added without one. */
    public static final Color DEFAULT_SERIES_COLOR = Color.BLUE.darker();

    private static final String STYLE_CLASS = "time-series-chart";
    private static final String STYLESHEET = Objects.requireNonNull(
                    TimeSeriesChart.class.getResource("time-series-chart.css"), "time-series-chart.css")
            .toExternalForm();
    private static final double PREF_WIDTH = 300;
    private static final double PREF_HEIGHT = 200;
    private static final double MIN_WIDTH = 200;
    private static final double MIN_HEIGHT = 150;

    private final Canvas canvas = new Canvas();
    private final SampleStore store = new SampleStore();
    private final ChartRenderer renderer = new ChartRenderer(store);
    private final ChartColours colours = new ChartColours(this, this::redraw);
    private final ReadOnlyObjectWrapper<TimeRange> timeRange =
            new ReadOnlyObjectWrapper<>(this, "timeRange", TimeRange.DEFAULT);
    private final ChartUnit unit;
    private ValueFormat format;
    private boolean showSeriesNames = true;
    private ContextMenu contextMenu;
    private CsvSaveDialog saveDialog;

    /** A chart of plain whole numbers. */
    public TimeSeriesChart() {
        this(ChartUnit.NONE, 0);
    }

    /** A chart of whole numbers in {@code unit}. */
    public TimeSeriesChart(ChartUnit unit) {
        this(unit, 0);
    }

    /** A chart of values in {@code unit} with {@code decimals} fixed decimal places. */
    public TimeSeriesChart(ChartUnit unit, int decimals) {
        this.unit = Objects.requireNonNull(unit, "unit");
        this.format = displayFormat(unit, decimals);
        getStyleClass().add(STYLE_CLASS);
        getChildren().add(canvas);
        setPrefSize(PREF_WIDTH, PREF_HEIGHT);
        setMinSize(MIN_WIDTH, MIN_HEIGHT);
        setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        setAccessibleText(Messages.PLOTTER_ACCESSIBLE_NAME);
        timeRange.addListener(observable -> redraw());
        setOnContextMenuRequested(this::showContextMenu);
    }

    /**
     * Adds a series. Add every series before the first values.
     *
     * @param key the name the series is found by later
     * @param name the name shown next to its newest value and in the CSV header
     * @param color the line's colour, or {@code null} for {@link #DEFAULT_SERIES_COLOR}
     * @param plotted whether the line is drawn
     */
    public void addSeries(String key, String name, Color color, boolean plotted) {
        store.addSeries(new Series(key, name, color == null ? DEFAULT_SERIES_COLOR : color, plotted));
        redraw();
    }

    /** Shows or hides the line of the series {@code key}. */
    public void setPlotted(String key, boolean plotted) {
        series(key).setPlotted(plotted);
        redraw();
    }

    /** Whether a change of value of the series {@code key} draws as a dashed jump, as a threshold line does. */
    public void setDashedTransitions(String key, boolean dashed) {
        series(key).setDashedTransitions(dashed);
        redraw();
    }

    /** Sets how many fixed decimal places the values carry. */
    public void setDecimals(int decimals) {
        format = displayFormat(unit, decimals);
        redraw();
    }

    /** Whether each newest value shows its series name above it; it does by default. */
    public void setShowSeriesNames(boolean show) {
        showSeriesNames = show;
        redraw();
    }

    /**
     * Adds a sample.
     *
     * @param timeMillis when the values were read, in epoch milliseconds
     * @param values one value per series, in the order the series were added
     */
    public void addValues(long timeMillis, long... values) {
        store.add(timeMillis, values);
        redraw();
    }

    /** Breaks every line at {@code timeMillis}, as when the connection drops. */
    public void addGap(long timeMillis) {
        store.addGap(timeMillis);
        redraw();
    }

    /** The newest value of the series {@code key}, or empty before its first value and right after a gap. */
    public OptionalLong lastValue(String key) {
        return series(key).lastValue();
    }

    /** The time of the newest sample, or empty before the first. */
    public OptionalLong lastTime() {
        return store.size() == 0 ? OptionalLong.empty() : OptionalLong.of(store.time(store.size() - 1));
    }

    /**
     * The time range shown; {@link TimeRange#ALL} by default. Several charts share one range through a
     * {@link TimeRangeChooser}.
     */
    public ReadOnlyObjectProperty<TimeRange> timeRangeProperty() {
        return timeRange.getReadOnlyProperty();
    }

    public TimeRange getTimeRange() {
        return timeRange.get();
    }

    public void setTimeRange(TimeRange range) {
        timeRange.set(range);
    }

    /** The writable range, for the chooser to bind and the context menu to set. */
    ObjectProperty<TimeRange> writableTimeRange() {
        return timeRange;
    }

    /** Asks for a CSV file and saves every sample there, as the context menu's "Save data as..." does. */
    public void saveDataAs() {
        if (saveDialog == null) {
            saveDialog = new CsvSaveDialog();
        }
        Scene scene = getScene();
        saveDialog.show(scene == null ? null : scene.getWindow(), store, format.decimals());
    }

    /** The CSS properties of a chart: a region's, and its four colours. */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return ChartColours.CSS_META_DATA;
    }

    @Override
    public String getUserAgentStylesheet() {
        return STYLESHEET;
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }

    ChartColours colours() {
        return colours;
    }

    private void redraw() {
        if (canvas.getWidth() <= 0 || canvas.getHeight() <= 0) {
            return;
        }
        TimeRange range = Objects.requireNonNullElse(timeRange.get(), TimeRange.DEFAULT);
        RenderOptions options = new RenderOptions(range, format, colours.palette(), showSeriesNames);
        renderer.render(canvas.getGraphicsContext2D(), canvas.getWidth(), canvas.getHeight(), options);
    }

    private void showContextMenu(ContextMenuEvent event) {
        if (contextMenu == null) {
            contextMenu = ChartContextMenu.create(timeRange, this::saveDataAs);
        }
        contextMenu.show(this, event.getScreenX(), event.getScreenY());
        event.consume();
    }

    private Series series(String key) {
        return store.find(key).orElseThrow(() -> new IllegalArgumentException("No series with the key " + key));
    }

    private static ValueFormat displayFormat(ChartUnit unit, int decimals) {
        return new ValueFormat(unit, decimals, Locale.getDefault(Locale.Category.FORMAT));
    }
}
