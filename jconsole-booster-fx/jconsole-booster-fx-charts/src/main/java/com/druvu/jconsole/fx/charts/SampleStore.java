package com.druvu.jconsole.fx.charts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The samples of one chart: the sample times and, for each series, the value read at each time. A gap (the connection
 * dropped) is a sample whose values are all {@link #GAP}.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class SampleStore {

    /** The value that marks a gap; no real reading takes it. */
    static final long GAP = Long.MIN_VALUE;

    private final LongList times = new LongList();
    private final List<Series> series = new ArrayList<>();

    static boolean isGap(long value) {
        return value == GAP;
    }

    /** Adds a series; every series comes before the first sample, so all of them hold a value for every time. */
    void addSeries(Series added) {
        if (find(added.key()).isPresent()) {
            throw new IllegalArgumentException("A series with the key " + added.key() + " exists already");
        }
        if (size() > 0) {
            throw new IllegalStateException("Add every series before the first values");
        }
        series.add(added);
    }

    /** The series in the order they were added: the first is the main one. */
    List<Series> series() {
        return Collections.unmodifiableList(series);
    }

    Optional<Series> find(String key) {
        return series.stream().filter(candidate -> candidate.key().equals(key)).findFirst();
    }

    /** Adds one sample: its time and one value per series, in the order the series were added. */
    void add(long time, long... values) {
        if (values.length != series.size()) {
            throw new IllegalArgumentException(
                    "Expected " + series.size() + " values, one per series, but got " + values.length);
        }
        times.add(time);
        for (int i = 0; i < values.length; i++) {
            series.get(i).add(values[i]);
        }
    }

    /** Adds a gap at {@code time}: the lines break there. */
    void addGap(long time) {
        times.add(time);
        for (Series each : series) {
            each.add(GAP);
        }
    }

    int size() {
        return times.size();
    }

    long time(int index) {
        return times.get(index);
    }

    /** The index of the first sample taken at or after {@code time}, or {@link #size()} when there is none. */
    int firstIndexAtOrAfter(long time) {
        for (int i = 0; i < size(); i++) {
            if (time(i) >= time) {
                return i;
            }
        }
        return size();
    }
}
