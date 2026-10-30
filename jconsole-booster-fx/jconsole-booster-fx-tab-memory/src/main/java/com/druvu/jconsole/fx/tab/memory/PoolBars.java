package com.druvu.jconsole.fx.tab.memory;

import com.druvu.jconsole.core.text.Messages;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * The memory pools at a glance, as the Swing JConsole draws them: a percentage scale, then one {@link PoolBar} per
 * pool, the heap pools above a "Heap" label and the non-heap pools above a "Non-Heap" one. A label turns red when a
 * pool of its group is above its threshold; a click on a bar or a label chooses that chart. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class PoolBars {

    private static final double BAR_GAP = 3;
    private static final double GROUP_GAP = 8;
    private static final double SCALE_WIDTH = 34;
    private static final double SCALE_LABEL_HEIGHT = 12;
    private static final int[] SCALE_PERCENTS = {0, 25, 50, 75, 100};

    private final Consumer<MemoryArea> onChoose;
    private final Map<MemoryArea, PoolBar> bars = new HashMap<>();
    private final HBox heapBars = new HBox(BAR_GAP);
    private final HBox nonHeapBars = new HBox(BAR_GAP);
    private final Label heapLabel;
    private final Label nonHeapLabel;
    private final HBox root;

    /** Pool bars that call {@code onChoose} with the area the user clicked. */
    PoolBars(Consumer<MemoryArea> onChoose) {
        this.onChoose = onChoose;
        heapLabel = groupLabel(Messages.HEAP, MemoryArea.HEAP);
        nonHeapLabel = groupLabel(Messages.NON_HEAP, MemoryArea.NON_HEAP);
        VBox heapGroup = group(heapBars, heapLabel);
        VBox nonHeapGroup = group(nonHeapBars, nonHeapLabel);
        Pane scale = scale();
        // nothing to draw before the first reading
        scale.visibleProperty().bind(heapGroup.visibleProperty().or(nonHeapGroup.visibleProperty()));
        scale.managedProperty().bind(scale.visibleProperty());
        root = new HBox(GROUP_GAP, scale, heapGroup, nonHeapGroup);
        root.setPadding(new Insets(SCALE_LABEL_HEIGHT / 2, 0, 0, 0));
        showSelected(MemoryArea.HEAP);
    }

    Node root() {
        return root;
    }

    /** Shows the pools' usage at the newest reading, adding a bar for each pool seen the first time. */
    void show(List<AreaUsage> pools) {
        boolean heapAbove = false;
        boolean nonHeapAbove = false;
        for (AreaUsage pool : pools) {
            bar(pool.area()).show(pool);
            boolean heap = pool.area().kind() == MemoryArea.Kind.HEAP_POOL;
            heapAbove |= heap && pool.isAboveThreshold();
            nonHeapAbove |= !heap && pool.isAboveThreshold();
        }
        heapLabel.setBackground(Background.fill(groupColor(heapAbove)));
        nonHeapLabel.setBackground(Background.fill(groupColor(nonHeapAbove)));
    }

    /** Marks the bar or label of {@code area}, the chart shown. */
    void showSelected(MemoryArea area) {
        bars.forEach((pool, bar) -> bar.setSelected(pool.equals(area)));
        heapLabel.setBorder(frame(area.equals(MemoryArea.HEAP)));
        nonHeapLabel.setBorder(frame(area.equals(MemoryArea.NON_HEAP)));
    }

    private PoolBar bar(MemoryArea pool) {
        return bars.computeIfAbsent(pool, this::newBar);
    }

    private PoolBar newBar(MemoryArea pool) {
        PoolBar bar = new PoolBar(() -> onChoose.accept(pool));
        (pool.kind() == MemoryArea.Kind.HEAP_POOL ? heapBars : nonHeapBars)
                .getChildren()
                .add(bar);
        return bar;
    }

    private Label groupLabel(String text, MemoryArea area) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(2, 4, 2, 4));
        // dark text on the green or red group colour, whatever the theme
        label.setTextFill(Color.BLACK);
        label.setCursor(Cursor.HAND);
        label.setOnMouseClicked(event -> onChoose.accept(area));
        return label;
    }

    /** A group's bars above its label; hidden until it has a bar. */
    private static VBox group(HBox groupBars, Label label) {
        VBox group = new VBox(BAR_GAP * 2, groupBars, label);
        group.visibleProperty().bind(Bindings.isNotEmpty(groupBars.getChildren()));
        group.managedProperty().bind(group.visibleProperty());
        return group;
    }

    /** The percentage scale beside the bars, each mark level with its share of a bar's height. */
    private static Pane scale() {
        Pane scale = new Pane();
        scale.setMinSize(SCALE_WIDTH, PoolBar.HEIGHT);
        scale.setPrefSize(SCALE_WIDTH, PoolBar.HEIGHT);
        for (int percent : SCALE_PERCENTS) {
            Label mark = new Label(percent + "%");
            mark.setAlignment(Pos.CENTER_RIGHT);
            mark.setPrefSize(SCALE_WIDTH, SCALE_LABEL_HEIGHT);
            mark.setStyle("-fx-font-size: 9px;");
            double level = PoolBar.HEIGHT - percent * PoolBar.HEIGHT / 100;
            mark.relocate(0, level - SCALE_LABEL_HEIGHT / 2);
            scale.getChildren().add(mark);
        }
        return scale;
    }

    private static Color groupColor(boolean aboveThreshold) {
        return aboveThreshold ? PoolBar.ABOVE_THRESHOLD_COLOR : PoolBar.USED_COLOR;
    }

    private static Border frame(boolean selected) {
        return new Border(new BorderStroke(
                selected ? PoolBar.SELECTED_COLOR : PoolBar.FRAME_COLOR,
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(selected ? 2 : 1)));
    }
}
