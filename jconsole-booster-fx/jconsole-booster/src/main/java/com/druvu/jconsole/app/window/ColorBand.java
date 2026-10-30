package com.druvu.jconsole.app.window;

import java.util.Optional;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

/**
 * The colour given with {@code -c=#RRGGBB[AA]}, shown as a thin band under the menu bar, so that windows on different
 * environments (production, staging) can be told apart at a glance.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@Slf4j
public final class ColorBand {

    private static final double HEIGHT = 4;

    private ColorBand() {}

    /** The colour of {@code hex}; empty when none was given or it is not a colour. */
    public static Optional<Color> parse(String hex) {
        if (hex == null || hex.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Color.web(hex.strip()));
        } catch (IllegalArgumentException notAColour) {
            log.warn("Ignoring the colour {}: not #RRGGBB or #RRGGBBAA", hex);
            return Optional.empty();
        }
    }

    /** The band in {@code color}. */
    static Region band(Color color) {
        Region band = new Region();
        band.setBackground(Background.fill(color));
        band.setMinHeight(HEIGHT);
        band.setPrefHeight(HEIGHT);
        band.setMaxHeight(HEIGHT);
        return band;
    }
}
