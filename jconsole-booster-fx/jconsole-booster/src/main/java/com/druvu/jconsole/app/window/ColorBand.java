package com.druvu.jconsole.app.window;

import java.util.Optional;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * The colour given with {@code -c=#RRGGBB[AA]}, shown as a thin band under the menu bar, so that windows on different
 * environments (production, staging) can be told apart at a glance.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class ColorBand {

    private static final System.Logger LOG = System.getLogger(ColorBand.class.getName());

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
            LOG.log(System.Logger.Level.WARNING, "Ignoring the colour " + hex + ": not #RRGGBB or #RRGGBBAA");
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
