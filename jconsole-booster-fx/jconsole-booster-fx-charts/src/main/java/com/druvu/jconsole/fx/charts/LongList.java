package com.druvu.jconsole.fx.charts;

import java.util.Arrays;
import java.util.Objects;

/**
 * A growable list of primitive longs, so hours of samples are kept without boxing each one.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class LongList {

    private static final int INITIAL_CAPACITY = 1024;

    private long[] values = new long[INITIAL_CAPACITY];
    private int size;

    void add(long value) {
        if (size == values.length) {
            values = Arrays.copyOf(values, size * 2);
        }
        values[size] = value;
        size++;
    }

    long get(int index) {
        Objects.checkIndex(index, size);
        return values[index];
    }

    int size() {
        return size;
    }
}
