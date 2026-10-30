package com.druvu.jconsole.fx.charts;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Picks the points a line is drawn through. With more than four samples per pixel the samples collapse: each run of
 * samples sharing a pixel column keeps only its first, lowest, highest and last value, so hours of samples draw fast
 * and no peak is lost.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class Decimator {

    /** From this many samples per pixel on, the samples of a pixel column collapse. */
    static final int COLLAPSE_THRESHOLD = 4;

    private Decimator() {}

    /** The points of {@code series} from the sample {@code firstVisible} on, for a plot {@code width} pixels wide. */
    static List<PlotPoint> points(SampleStore store, Series series, int firstVisible, double width) {
        int perPixel = (int) ((store.size() - firstVisible) / width);
        List<PlotPoint> points = new ArrayList<>();
        if (perPixel < COLLAPSE_THRESHOLD) {
            for (int i = firstVisible; i < store.size(); i++) {
                points.add(point(store, series, i));
            }
            return points;
        }
        for (int columnStart = firstVisible; columnStart < store.size(); columnStart += perPixel) {
            addColumn(points, store, series, columnStart, Math.min(columnStart + perPixel, store.size()));
        }
        return points;
    }

    /** Adds the samples [from, to) of one pixel column: each unbroken run collapses, and every gap stays. */
    private static void addColumn(List<PlotPoint> points, SampleStore store, Series series, int from, int to) {
        int runStart = -1;
        for (int i = from; i < to; i++) {
            if (SampleStore.isGap(series.value(i))) {
                if (runStart >= 0) {
                    addRun(points, store, series, runStart, i);
                    runStart = -1;
                }
                points.add(point(store, series, i));
            } else if (runStart < 0) {
                runStart = i;
            }
        }
        if (runStart >= 0) {
            addRun(points, store, series, runStart, to);
        }
    }

    /** Adds the first, lowest, highest and last sample of the run [from, to), in time order and each once. */
    private static void addRun(List<PlotPoint> points, SampleStore store, Series series, int from, int to) {
        int lowest = from;
        int highest = from;
        for (int i = from + 1; i < to; i++) {
            if (series.value(i) < series.value(lowest)) {
                lowest = i;
            }
            if (series.value(i) > series.value(highest)) {
                highest = i;
            }
        }
        IntStream.of(from, lowest, highest, to - 1)
                .sorted()
                .distinct()
                .forEach(index -> points.add(point(store, series, index)));
    }

    private static PlotPoint point(SampleStore store, Series series, int index) {
        return new PlotPoint(store.time(index), series.value(index));
    }
}
