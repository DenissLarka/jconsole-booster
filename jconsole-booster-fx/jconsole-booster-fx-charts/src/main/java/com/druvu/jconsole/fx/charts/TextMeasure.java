package com.druvu.jconsole.fx.charts;

import java.util.Collection;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Measures how wide texts draw in a font, so that the margins fit their labels.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class TextMeasure {

    private final Text probe = new Text();

    /** The width of the widest of {@code texts} in {@code font}, or 0 when there are none. */
    double widest(Collection<String> texts, Font font) {
        probe.setFont(font);
        double widest = 0;
        for (String text : texts) {
            probe.setText(text);
            widest = Math.max(widest, probe.getLayoutBounds().getWidth());
        }
        return widest;
    }
}
