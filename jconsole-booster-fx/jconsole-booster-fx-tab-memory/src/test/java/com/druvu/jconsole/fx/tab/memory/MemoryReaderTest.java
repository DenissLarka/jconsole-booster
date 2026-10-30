package com.druvu.jconsole.fx.tab.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.AttributeList;
import javax.management.ObjectName;
import org.testng.annotations.Test;

/** Reading this JVM's own memory: the areas and their order, the pools' kinds and thresholds, the collectors. */
public class MemoryReaderTest {

    @Test
    public void readsTheHeapAndTheNonHeapMemoryFirst() throws IOException {
        MemoryReading reading = new MemoryReader(new LocalJvmConnection().connection()).read();

        assertThat(reading.areas()).extracting(AreaUsage::area).startsWith(MemoryArea.HEAP, MemoryArea.NON_HEAP);
        assertThat(reading.usage(MemoryArea.HEAP).orElseThrow().used()).isPositive();
    }

    @Test
    public void classifiesEachPoolByItsType() throws IOException {
        MemoryReading reading = new MemoryReader(new LocalJvmConnection().connection()).read();

        List<MemoryArea> expected = ManagementFactory.getMemoryPoolMXBeans().stream()
                .map(pool -> MemoryArea.pool(pool.getName(), pool.getType() == MemoryType.HEAP))
                .toList();
        assertThat(reading.pools()).extracting(AreaUsage::area).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void listsTheHeapPoolsBeforeTheNonHeapPoolsEachByName() throws IOException {
        MemoryReading reading = new MemoryReader(new LocalJvmConnection().connection()).read();

        assertThat(reading.areas()).extracting(AreaUsage::area).isSortedAccordingTo(MemoryArea.ORDER);
        assertThat(reading.pools()).extracting(usage -> usage.area().kind()).contains(MemoryArea.Kind.HEAP_POOL);
    }

    @Test
    public void readsAPoolsUsageThreshold() throws IOException {
        MemoryPoolMXBean pool = ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(MemoryPoolMXBean::isUsageThresholdSupported)
                .filter(candidate -> candidate.getUsage().getUsed() > 1)
                .findFirst()
                .orElseThrow();
        MemoryArea area = MemoryArea.pool(pool.getName(), pool.getType() == MemoryType.HEAP);
        pool.setUsageThreshold(1);
        try {
            AreaUsage usage = new MemoryReader(new LocalJvmConnection().connection())
                    .read()
                    .usage(area)
                    .orElseThrow();

            assertThat(usage.threshold()).isEqualTo(1);
            assertThat(usage.isAboveThreshold()).isTrue();
        } finally {
            // zero turns the threshold off again
            pool.setUsageThreshold(0);
        }
    }

    @Test
    public void aPoolWithoutAThresholdReadsNone() throws IOException {
        MemoryPoolMXBean pool = ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(candidate -> !candidate.isUsageThresholdSupported())
                .findFirst()
                .orElseThrow();
        MemoryArea area = MemoryArea.pool(pool.getName(), pool.getType() == MemoryType.HEAP);

        AreaUsage usage = new MemoryReader(new LocalJvmConnection().connection())
                .read()
                .usage(area)
                .orElseThrow();

        assertThat(usage.threshold()).isEqualTo(AreaUsage.NO_THRESHOLD);
        assertThat(usage.hasThreshold()).isFalse();
    }

    @Test
    public void leavesOutAPoolWhoseUsageCannotBeRead() throws IOException {
        LocalJvmConnection connection = new LocalJvmConnection();
        connection.answer("getAttributes", args -> {
            boolean usageRead = List.of((String[]) args[1]).contains("Usage");
            return usageRead
                    ? new AttributeList()
                    : ManagementFactory.getPlatformMBeanServer()
                            .getAttributes((ObjectName) args[0], (String[]) args[1]);
        });

        MemoryReading reading = new MemoryReader(connection.connection()).read();

        assertThat(reading.areas()).extracting(AreaUsage::area).containsExactly(MemoryArea.HEAP, MemoryArea.NON_HEAP);
    }

    @Test
    public void findsThePoolsOnce() throws IOException {
        AtomicInteger lookups = new AtomicInteger();
        LocalJvmConnection connection = new LocalJvmConnection();
        connection.answer("getMBeans", args -> {
            lookups.incrementAndGet();
            return new LocalJvmConnection().connection().getMBeans((String) args[0]);
        });
        MemoryReader reader = new MemoryReader(connection.connection());

        reader.read();
        reader.read();

        assertThat(lookups).hasValue(1);
    }

    @Test
    public void readsEveryCollector() throws IOException {
        MemoryReading reading = new MemoryReader(new LocalJvmConnection().connection()).read();

        assertThat(reading.collectors())
                .extracting(CollectorTime::name)
                .containsExactlyElementsOf(ManagementFactory.getGarbageCollectorMXBeans().stream()
                        .map(GarbageCollectorMXBean::getName)
                        .toList());
    }
}
