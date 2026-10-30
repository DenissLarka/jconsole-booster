package com.druvu.jconsole.fx.charts;

import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The value axis of a chart: bounds rounded out to readable numbers around the visible values, and the ticks between
 * them.
 *
 * @param min the value at the bottom of the axis
 * @param max the value at the top of the axis, always above {@code min}
 * @param ticks the tick values, ascending, from {@code min} to {@code max}
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record ValueScale(long min, long max, List<Long> ticks) {

    /** A bottom more than this many times below the top reads best against zero. */
    private static final long ZERO_BASED_RATIO = 4;

    ValueScale {
        ticks = List.copyOf(ticks);
    }

    /**
     * The scale for the samples from {@code firstVisible} on. The first series always sets it; with byte sizes it alone
     * does, otherwise every drawn series joins it.
     */
    static ValueScale forVisible(SampleStore store, int firstVisible, ChartUnit unit) {
        LongSummaryStatistics visible = new LongSummaryStatistics();
        List<Series> all = store.series();
        for (int index = 0; index < all.size(); index++) {
            Series series = all.get(index);
            if (setsScale(index, series, unit)) {
                for (int i = firstVisible; i < store.size(); i++) {
                    long value = series.value(i);
                    if (!SampleStore.isGap(value)) {
                        visible.accept(value);
                    }
                }
            }
        }
        return of(visible);
    }

    private static boolean setsScale(int index, Series series, ChartUnit unit) {
        if (index == 0) {
            return true;
        }
        // memory charts carry Max far above Used; scaling to it would flatten the Used line
        return unit != ChartUnit.BYTES && series.isPlotted();
    }

    /** The scale that holds the {@code visible} values; with none it runs from 0 to 2. */
    static ValueScale of(LongSummaryStatistics visible) {
        long max = roundUp(Math.max(1, visible.getMax()));
        long min = visible.getCount() == 0 ? 0 : visible.getMin();
        if (min > 0) {
            min = max / min > ZERO_BASED_RATIO ? 0 : roundDown(min);
        }
        return new ValueScale(min, max, ticks(min, max));
    }

    /** The next number above a positive {@code value} with one significant digit: 37 to 40, 95 to 100, 100 to 200. */
    static long roundUp(long value) {
        long magnitude = magnitude(value);
        return (value / magnitude + 1) * magnitude;
    }

    /** A positive {@code value} cut to its first significant digit: 37 to 30, 1234 to 1000. */
    static long roundDown(long value) {
        long magnitude = magnitude(value);
        return value / magnitude * magnitude;
    }

    /** The power of ten of the leading digit of a positive {@code value}: 1 for 7, 100 for 345. */
    private static long magnitude(long value) {
        return (long) Math.pow(10, (int) Math.log10(value));
    }

    private static List<Long> ticks(long min, long max) {
        int steps = stepsFor(max);
        SortedSet<Long> ticks = new TreeSet<>();
        ticks.add(min);
        for (int i = 0; i < steps; i++) {
            long tick = i * max / steps;
            if (tick > min) {
                ticks.add(tick);
            }
        }
        ticks.add(max);
        return List.copyOf(ticks);
    }

    /**
     * How many steps divide the axis: picked by the leading digit of the top so that the ticks fall on round numbers.
     */
    private static int stepsFor(long max) {
        return switch (Long.toString(max).charAt(0)) {
            case '2', '4' -> 4;
            case '3', '6' -> 6;
            case '7' -> 7;
            case '8' -> 8;
            case '9' -> 3;
            default -> 5;
        };
    }

    /** Where {@code value} sits between the bounds: 0 at the bottom, 1 at the top. */
    double fraction(long value) {
        return (double) (value - min) / (max - min);
    }

    boolean contains(long value) {
        return value >= min && value <= max;
    }
}
