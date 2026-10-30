package com.druvu.jconsole.fx.tab.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The pools' usage bars on the FX thread: one bar per pool, red above the threshold, the tooltip, a click. */
public class PoolBarsTest {

    private static final MemoryArea EDEN = MemoryArea.pool("Eden", true);
    private static final MemoryArea OLD_GEN = MemoryArea.pool("Old Gen", true);
    private static final MemoryArea METASPACE = MemoryArea.pool("Metaspace", false);

    @BeforeClass
    public void startFx() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void showsOneBarPerPool() {
        int barCount = FxTestToolkit.call(() -> {
            PoolBars bars = new PoolBars(area -> {});
            bars.show(List.of(usage(EDEN, 10, 0), usage(OLD_GEN, 20, 0), usage(METASPACE, 30, 0)));
            bars.show(List.of(usage(EDEN, 15, 0), usage(OLD_GEN, 25, 0), usage(METASPACE, 35, 0)));
            return bars.root().lookupAll("." + PoolBar.STYLE_CLASS).size();
        });

        assertThat(barCount).isEqualTo(3);
    }

    @Test
    public void thePartAboveTheThresholdIsRed() {
        double redAbove = FxTestToolkit.call(() -> redHeight(bar(usage(OLD_GEN, 80, 50))));
        double redBelow = FxTestToolkit.call(() -> redHeight(bar(usage(OLD_GEN, 40, 50))));

        assertThat(redAbove).isEqualTo(30.0);
        assertThat(redBelow).isZero();
    }

    @Test
    public void aGroupWithAPoolAboveItsThresholdHasARedLabel() {
        Color heapColor = FxTestToolkit.call(() -> {
            PoolBars bars = new PoolBars(area -> {});
            bars.show(List.of(usage(EDEN, 10, 0), usage(OLD_GEN, 80, 50), usage(METASPACE, 30, 0)));
            return (Color) groupLabel(bars, "Heap")
                    .getBackground()
                    .getFills()
                    .getFirst()
                    .getFill();
        });

        assertThat(heapColor).isEqualTo(PoolBar.ABOVE_THRESHOLD_COLOR);
    }

    @Test
    public void aClickOnABarOrALabelChoosesItsChart() {
        AtomicReference<MemoryArea> chosen = new AtomicReference<>();
        PoolBars bars = FxTestToolkit.call(() -> new PoolBars(chosen::set));

        FxTestToolkit.runAndWait(() -> {
            bars.show(List.of(usage(OLD_GEN, 10, 0)));
            click(bars.root().lookup("." + PoolBar.STYLE_CLASS));
        });
        assertThat(chosen).hasValue(OLD_GEN);

        FxTestToolkit.runAndWait(() -> click(groupLabel(bars, "Non-Heap")));
        assertThat(chosen).hasValue(MemoryArea.NON_HEAP);
    }

    @Test
    public void theTooltipSaysHowFullThePoolIs() {
        String withThreshold = FxTestToolkit.call(() -> PoolBar.tooltipText(usage(OLD_GEN, 45, 80)));
        String withoutMax = FxTestToolkit.call(
                () -> PoolBar.tooltipText(new AreaUsage(METASPACE, 45, 50, -1, AreaUsage.NO_THRESHOLD)));

        assertThat(withThreshold).isEqualTo("Memory Pool \"Old Gen\"\nUsed: 45%\nUsage Threshold: 80%");
        assertThat(withoutMax).isEqualTo("Memory Pool \"Metaspace\"");
    }

    /** A usage of a pool of 100 bytes at most. */
    private static AreaUsage usage(MemoryArea pool, long used, long threshold) {
        return new AreaUsage(pool, used, used, 100, threshold);
    }

    private static PoolBar bar(AreaUsage usage) {
        PoolBar bar = new PoolBar(() -> {});
        bar.show(usage);
        return bar;
    }

    private static double redHeight(PoolBar bar) {
        return bar.getChildren().stream()
                .map(Rectangle.class::cast)
                .filter(part -> PoolBar.ABOVE_THRESHOLD_COLOR.equals(part.getFill()))
                .mapToDouble(Rectangle::getHeight)
                .sum();
    }

    private static Label groupLabel(PoolBars bars, String text) {
        return bars.root().lookupAll(".label").stream()
                .map(Label.class::cast)
                .filter(label -> text.equals(label.getText()))
                .findFirst()
                .orElseThrow();
    }

    /** What a click of the primary button does. */
    private static void click(Node node) {
        node.fireEvent(new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                0,
                0,
                0,
                0,
                MouseButton.PRIMARY,
                1,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                true,
                null));
    }
}
