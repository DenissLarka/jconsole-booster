package com.druvu.jconsole.fx.charts;

import java.util.Objects;
import java.util.OptionalLong;
import javafx.scene.paint.Color;

/**
 * One line of a chart: its key, name and colour, whether it is drawn, and its values, one per sample time.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class Series {

    private final String key;
    private final String name;
    private final Color color;
    private final LongList values = new LongList();
    private boolean plotted;
    private boolean dashedTransitions;

    Series(String key, String name, Color color, boolean plotted) {
        this.key = Objects.requireNonNull(key, "key");
        this.name = Objects.requireNonNull(name, "name");
        this.color = Objects.requireNonNull(color, "color");
        this.plotted = plotted;
    }

    String key() {
        return key;
    }

    String name() {
        return name;
    }

    Color color() {
        return color;
    }

    boolean isPlotted() {
        return plotted;
    }

    void setPlotted(boolean plotted) {
        this.plotted = plotted;
    }

    /** Whether a change of value draws as a dashed jump, as a threshold line does. */
    boolean hasDashedTransitions() {
        return dashedTransitions;
    }

    void setDashedTransitions(boolean dashedTransitions) {
        this.dashedTransitions = dashedTransitions;
    }

    void add(long value) {
        values.add(value);
    }

    long value(int index) {
        return values.get(index);
    }

    /** The newest value, or empty before the first sample and right after a gap. */
    OptionalLong lastValue() {
        if (values.size() == 0) {
            return OptionalLong.empty();
        }
        long last = values.get(values.size() - 1);
        return SampleStore.isGap(last) ? OptionalLong.empty() : OptionalLong.of(last);
    }
}
