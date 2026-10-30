package com.druvu.jconsole.fx.tab.memory;

import com.druvu.jconsole.core.vm.JmxDataAccess;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

/**
 * Reads the target JVM's memory: the heap, the non-heap memory, each memory pool and the garbage collectors. Every call
 * is a JMX round trip, so this runs on the update thread, never on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class MemoryReader {

    private static final String POOL_TYPE = "MemoryPool";
    private static final String TYPE = "Type";
    private static final String USAGE = "Usage";
    private static final String USAGE_THRESHOLD = "UsageThreshold";

    private final JmxDataAccess connection;

    /** The memory pools, found at the first reading: a JVM keeps its pools for its whole life. */
    private List<MemoryPool> pools;

    MemoryReader(JmxDataAccess connection) {
        this.connection = connection;
    }

    /**
     * Reads the memory once.
     *
     * @throws IOException the connection is gone; an MXBean proxy may also report it in an
     *     {@link java.lang.reflect.UndeclaredThrowableException}
     */
    MemoryReading read() throws IOException {
        if (pools == null) {
            pools = findPools();
        }
        long timeMillis = System.currentTimeMillis();
        MemoryMXBean memory = connection.getMemoryMXBean();
        List<AreaUsage> areas = new ArrayList<>();
        areas.add(AreaUsage.of(MemoryArea.HEAP, memory.getHeapMemoryUsage(), AreaUsage.NO_THRESHOLD));
        areas.add(AreaUsage.of(MemoryArea.NON_HEAP, memory.getNonHeapMemoryUsage(), AreaUsage.NO_THRESHOLD));
        for (MemoryPool pool : pools) {
            readPool(pool).ifPresent(areas::add);
        }
        return new MemoryReading(timeMillis, areas, readCollectors());
    }

    /** The target's memory pools, the heap pools first, each group by name. */
    private List<MemoryPool> findPools() throws IOException {
        List<MemoryPool> found = new ArrayList<>();
        for (ObjectName name : connection.getMBeans("java.lang").keySet()) {
            if (POOL_TYPE.equals(name.getKeyProperty("type"))) {
                boolean heap = attribute(name, TYPE)
                        .map(type -> MemoryType.HEAP.name().equals(type))
                        .orElse(false);
                found.add(new MemoryPool(name, MemoryArea.pool(name.getKeyProperty("name"), heap)));
            }
        }
        found.sort(Comparator.comparing(MemoryPool::area, MemoryArea.ORDER));
        return found;
    }

    /** The pool's usage, or empty when the pool is gone or no longer valid. */
    private Optional<AreaUsage> readPool(MemoryPool pool) throws IOException {
        AttributeList values = connection.getAttributes(pool.objectName(), new String[] {USAGE, USAGE_THRESHOLD});
        // a pool without a usage threshold has no such attribute: reading it fails, so the list leaves it out
        long threshold = value(values, USAGE_THRESHOLD).map(Long.class::cast).orElse(AreaUsage.NO_THRESHOLD);
        return value(values, USAGE)
                .map(CompositeData.class::cast)
                .map(MemoryUsage::from)
                .map(usage -> AreaUsage.of(pool.area(), usage, threshold));
    }

    private List<CollectorTime> readCollectors() throws IOException {
        List<CollectorTime> collectors = new ArrayList<>();
        for (GarbageCollectorMXBean collector : connection.getGarbageCollectorMXBeans()) {
            collectors.add(new CollectorTime(
                    collector.getName(), collector.getCollectionCount(), collector.getCollectionTime()));
        }
        return collectors;
    }

    private Optional<Object> attribute(ObjectName name, String attribute) throws IOException {
        return value(connection.getAttributes(name, new String[] {attribute}), attribute);
    }

    /** The value named {@code name} in {@code values}, which is null when the MBean could not be read at all. */
    private static Optional<Object> value(AttributeList values, String name) {
        if (values == null) {
            return Optional.empty();
        }
        return values.asList().stream()
                .filter(attribute -> attribute.getName().equals(name))
                .map(Attribute::getValue)
                // a pool that is no longer valid reports its usage as null
                .filter(Objects::nonNull)
                .findFirst();
    }

    /** A memory pool of the target, and the area it is charted as. */
    private record MemoryPool(ObjectName objectName, MemoryArea area) {}
}
