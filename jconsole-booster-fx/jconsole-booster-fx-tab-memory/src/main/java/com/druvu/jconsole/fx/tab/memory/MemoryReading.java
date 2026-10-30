package com.druvu.jconsole.fx.tab.memory;

import java.util.List;
import java.util.Optional;

/**
 * One reading of the target JVM's memory, ready for the screen.
 *
 * @param timeMillis when it was read, in epoch milliseconds
 * @param areas the usage of each area, in the chart chooser's order; a pool that could not be read is missing
 * @param collectors the garbage collectors' counts and times
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record MemoryReading(long timeMillis, List<AreaUsage> areas, List<CollectorTime> collectors) {

    MemoryReading {
        areas = List.copyOf(areas);
        collectors = List.copyOf(collectors);
    }

    /** The usage of {@code area}, or empty when this reading has none. */
    Optional<AreaUsage> usage(MemoryArea area) {
        return areas.stream().filter(usage -> usage.area().equals(area)).findFirst();
    }

    /** The usage of the memory pools only, the heap pools first. */
    List<AreaUsage> pools() {
        return areas.stream().filter(usage -> usage.area().isPool()).toList();
    }
}
