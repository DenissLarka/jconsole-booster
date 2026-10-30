package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.util.List;
import java.util.OptionalLong;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Renders charts with the JavaFX toolkit running, in theme colours taken from CSS. */
public class TimeSeriesChartTest {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 400;
    private static final Color DARK_PLOT = Color.web("#101820");
    /** The AtlantaFX colour names the chart's stylesheet looks up, as a dark theme sets them. */
    private static final String DARK_THEME = "-color-bg-subtle: #101820; -color-border-default: #808080;"
            + " -color-fg-default: #f0f0f0; -color-border-muted: #303840;";

    private static final double COLOUR_TOLERANCE = 0.15;

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void drawsTheShownLinesOnAThemedPlot() {
        Image image = FxTestToolkit.call(() -> {
            TimeSeriesChart chart = new TimeSeriesChart(ChartUnit.BYTES);
            chart.addSeries("used", "Used", Color.RED, true);
            chart.addSeries("max", "Max", Color.LIME, false);
            long start = System.currentTimeMillis() - Duration.ofMinutes(10).toMillis();
            for (int i = 0; i < 600; i++) {
                chart.addValues(start + i * 1000L, 1_000_000L + i * 10_000L, 50_000_000L);
            }
            return snapshot(chart);
        });

        assertThat(pixelsOf(image, Color.RED)).as("pixels of the shown line").isGreaterThan(50);
        assertThat(pixelsOf(image, Color.LIME)).as("pixels of the hidden line").isZero();
        assertThat(pixelsOf(image, DARK_PLOT)).as("pixels of the themed plot").isGreaterThan(WIDTH * HEIGHT / 4);
    }

    @Test
    public void drawsHoursOfSamplesWithAGap() {
        Image image = FxTestToolkit.call(() -> {
            TimeSeriesChart chart = new TimeSeriesChart(ChartUnit.PERCENT, 1);
            chart.addSeries("cpu", "CPU", Color.RED, true);
            chart.setTimeRange(TimeRange.SIX_HOURS);
            long start = System.currentTimeMillis() - Duration.ofHours(6).toMillis();
            for (int i = 0; i < 6 * 3600; i++) {
                long time = start + i * 1000L;
                if (i == 3 * 3600) {
                    chart.addGap(time);
                } else {
                    chart.addValues(time, i % 1000);
                }
            }
            return snapshot(chart);
        });

        assertThat(pixelsOf(image, Color.RED)).isGreaterThan(50);
    }

    @Test
    public void reportsTheNewestValueAndTime() {
        List<OptionalLong> newest = FxTestToolkit.call(() -> {
            TimeSeriesChart chart = new TimeSeriesChart();
            chart.addSeries("threads", "Live threads", null, true);
            chart.addValues(1000, 5);
            chart.addValues(2000, 7);
            OptionalLong beforeGap = chart.lastValue("threads");
            chart.addGap(3000);
            return List.of(beforeGap, chart.lastValue("threads"), chart.lastTime());
        });

        assertThat(newest).containsExactly(OptionalLong.of(7), OptionalLong.empty(), OptionalLong.of(3000));
    }

    @Test
    public void anUnknownSeriesKeyIsRejected() {
        Throwable thrown = FxTestToolkit.call(() -> catchThrowable(() -> new TimeSeriesChart().lastValue("nothing")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    private static Image snapshot(TimeSeriesChart chart) {
        StackPane root = new StackPane(chart);
        root.setStyle(DARK_THEME);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        return scene.snapshot(null);
    }

    private static int pixelsOf(Image image, Color colour) {
        PixelReader reader = image.getPixelReader();
        int count = 0;
        for (int y = 0; y < (int) image.getHeight(); y++) {
            for (int x = 0; x < (int) image.getWidth(); x++) {
                if (isClose(reader.getColor(x, y), colour)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean isClose(Color actual, Color expected) {
        return Math.abs(actual.getRed() - expected.getRed()) < COLOUR_TOLERANCE
                && Math.abs(actual.getGreen() - expected.getGreen()) < COLOUR_TOLERANCE
                && Math.abs(actual.getBlue() - expected.getBlue()) < COLOUR_TOLERANCE;
    }
}
