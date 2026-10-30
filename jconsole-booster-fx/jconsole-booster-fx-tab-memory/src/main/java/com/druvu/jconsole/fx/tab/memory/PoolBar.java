package com.druvu.jconsole.fx.tab.memory;

import com.druvu.jconsole.core.text.Messages;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * The usage bar of one memory pool: the used share of its maximum in green, the part above its usage threshold in red,
 * and the free room above the threshold in light red. A pool without a maximum shows an empty frame. A click asks for
 * the pool's chart. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class PoolBar extends Pane {

    static final String STYLE_CLASS = "memory-pool-bar";
    static final double WIDTH = 22;
    static final double HEIGHT = 100;
    static final Color USED_COLOR = Color.rgb(100, 255, 100);
    static final Color ABOVE_THRESHOLD_COLOR = Color.RED;
    static final Color FRAME_COLOR = Color.GRAY;
    static final Color SELECTED_COLOR = Color.BLUE;

    private static final Color FREE_COLOR = Color.rgb(210, 255, 210);
    private static final Color FREE_ABOVE_THRESHOLD_COLOR = Color.rgb(255, 210, 210);

    private final Rectangle free = new Rectangle(WIDTH, 0, FREE_COLOR);
    private final Rectangle freeAboveThreshold = new Rectangle(WIDTH, 0, FREE_ABOVE_THRESHOLD_COLOR);
    private final Rectangle used = new Rectangle(WIDTH, 0, USED_COLOR);
    private final Rectangle usedAboveThreshold = new Rectangle(WIDTH, 0, ABOVE_THRESHOLD_COLOR);
    private final Rectangle frame = new Rectangle(WIDTH, HEIGHT, Color.TRANSPARENT);
    private final Tooltip tooltip = new Tooltip();

    PoolBar(Runnable onClick) {
        getStyleClass().add(STYLE_CLASS);
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        frame.setStroke(FRAME_COLOR);
        getChildren().addAll(free, freeAboveThreshold, used, usedAboveThreshold, frame);
        Tooltip.install(this, tooltip);
        setCursor(Cursor.HAND);
        setOnMouseClicked(event -> onClick.run());
    }

    /** Shows the pool's usage at the newest reading. */
    void show(AreaUsage usage) {
        tooltip.setText(tooltipText(usage));
        if (!usage.hasMax()) {
            fill(free, 0, 0);
            fill(freeAboveThreshold, 0, 0);
            fill(used, 0, 0);
            fill(usedAboveThreshold, 0, 0);
            return;
        }
        double usedLevel = usage.shareOfMax(usage.used()) * HEIGHT;
        double thresholdLevel = usage.hasThreshold() ? usage.shareOfMax(usage.threshold()) * HEIGHT : HEIGHT;
        fill(free, 0, HEIGHT);
        fill(freeAboveThreshold, thresholdLevel, HEIGHT);
        fill(used, 0, Math.min(usedLevel, thresholdLevel));
        fill(usedAboveThreshold, thresholdLevel, Math.max(usedLevel, thresholdLevel));
    }

    /** Marks the bar as the one whose chart is shown. */
    void setSelected(boolean selected) {
        frame.setStroke(selected ? SELECTED_COLOR : FRAME_COLOR);
        frame.setStrokeWidth(selected ? 2 : 1);
    }

    /** The pool's name and how full it is, such as {@code Used: 45%} and {@code Usage Threshold: 80%}. */
    static String tooltipText(AreaUsage usage) {
        StringBuilder text = new StringBuilder(usage.area().name());
        if (usage.hasMax()) {
            text.append('\n')
                    .append(Messages.USED)
                    .append(": ")
                    .append(usage.percentOfMax(usage.used()))
                    .append('%');
            if (usage.hasThreshold()) {
                text.append('\n')
                        .append(Messages.USAGE_THRESHOLD)
                        .append(": ")
                        .append(usage.percentOfMax(usage.threshold()))
                        .append('%');
            }
        }
        return text.toString();
    }

    /** Fills {@code part} between two heights, measured up from the bar's bottom. */
    private static void fill(Rectangle part, double from, double to) {
        part.setY(HEIGHT - to);
        part.setHeight(to - from);
    }
}
