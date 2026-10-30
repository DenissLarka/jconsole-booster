package com.druvu.jconsole.fx.tab.memory;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.text.Resources;
import java.util.Comparator;

/**
 * A part of the target JVM's memory that has its own chart: the heap, the non-heap memory, or one memory pool.
 *
 * @param name the name shown in the chart chooser and in the pool bar's tooltip
 * @param kind which part it is
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record MemoryArea(String name, Kind kind) {

    /** The parts of memory, in the order the chart chooser lists them. */
    enum Kind {
        HEAP,
        NON_HEAP,
        HEAP_POOL,
        NON_HEAP_POOL
    }

    static final MemoryArea HEAP = new MemoryArea(Messages.HEAP_MEMORY_USAGE, Kind.HEAP);
    static final MemoryArea NON_HEAP = new MemoryArea(Messages.NON_HEAP_MEMORY_USAGE, Kind.NON_HEAP);

    /** The chooser's order: heap, non-heap, then the heap pools and the non-heap pools, each group by name. */
    static final Comparator<MemoryArea> ORDER =
            Comparator.comparing(MemoryArea::kind).thenComparing(MemoryArea::name);

    /** The memory pool named {@code poolName}, as the JVM names it. */
    static MemoryArea pool(String poolName, boolean heap) {
        return new MemoryArea(
                Resources.format(Messages.MEMORY_POOL_LABEL, poolName), heap ? Kind.HEAP_POOL : Kind.NON_HEAP_POOL);
    }

    boolean isPool() {
        return kind == Kind.HEAP_POOL || kind == Kind.NON_HEAP_POOL;
    }

    /** The chart chooser shows an area by its name. */
    @Override
    public String toString() {
        return name;
    }
}
