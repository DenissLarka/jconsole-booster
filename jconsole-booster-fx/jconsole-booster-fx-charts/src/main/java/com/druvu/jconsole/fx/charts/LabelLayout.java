package com.druvu.jconsole.fx.charts;

import java.util.Comparator;
import java.util.stream.IntStream;

/**
 * Keeps the current-value labels at the right margin apart: each label sits as close to the end of its line as the
 * others allow.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class LabelLayout {

    private LabelLayout() {}

    /**
     * Places labels at least {@code minGap} apart between {@code top} and {@code bottom} (screen coordinates, y grows
     * downwards).
     *
     * @param wanted where each label would sit on its own
     * @return where each label sits, in the order of {@code wanted}
     */
    static double[] spread(double[] wanted, double minGap, double top, double bottom) {
        int[] topDown = IntStream.range(0, wanted.length)
                .boxed()
                .sorted(Comparator.comparingDouble(index -> wanted[index]))
                .mapToInt(Integer::intValue)
                .toArray();
        double[] placed = new double[wanted.length];
        // downwards: each label at least minGap below the one above it
        double above = top - minGap;
        for (int index : topDown) {
            placed[index] = Math.max(wanted[index], above + minGap);
            above = placed[index];
        }
        // then upwards: no label below the bottom, still minGap apart
        double below = bottom + minGap;
        for (int k = topDown.length - 1; k >= 0; k--) {
            int index = topDown[k];
            placed[index] = Math.min(placed[index], below - minGap);
            below = placed[index];
        }
        return placed;
    }
}
