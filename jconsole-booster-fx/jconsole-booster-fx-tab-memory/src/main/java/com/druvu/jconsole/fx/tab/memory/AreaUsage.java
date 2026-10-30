package com.druvu.jconsole.fx.tab.memory;

import java.lang.management.MemoryUsage;

/**
 * How much of a memory area is in use at one reading, in bytes.
 *
 * @param area the memory area
 * @param used the bytes in use
 * @param committed the bytes the JVM holds for the area
 * @param max the most bytes the area may grow to, or -1 when it has no limit
 * @param threshold the pool's usage threshold, or {@link #NO_THRESHOLD}: zero means the threshold is off
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record AreaUsage(MemoryArea area, long used, long committed, long max, long threshold) {

    /** The threshold of an area that has none: the heap, the non-heap memory, or a pool without one. */
    static final long NO_THRESHOLD = 0;

    private static final long PERCENT = 100;

    static AreaUsage of(MemoryArea area, MemoryUsage usage, long threshold) {
        return new AreaUsage(area, usage.getUsed(), usage.getCommitted(), usage.getMax(), threshold);
    }

    boolean hasMax() {
        return max > 0;
    }

    boolean hasThreshold() {
        return threshold > 0;
    }

    /** Whether the area uses more than its threshold, the state its bar shows in red. */
    boolean isAboveThreshold() {
        return hasThreshold() && used > threshold;
    }

    /** {@code bytes} as a whole percentage of the maximum; only meaningful when {@link #hasMax()}. */
    long percentOfMax(long bytes) {
        return bytes * PERCENT / max;
    }

    /** {@code bytes} as a share of the maximum between 0 and 1; only meaningful when {@link #hasMax()}. */
    double shareOfMax(long bytes) {
        return Math.clamp((double) bytes / max, 0.0, 1.0);
    }
}
