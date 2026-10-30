package com.druvu.jconsole.fx.tab.overview;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import org.testng.annotations.Test;

/** Reading this JVM's overview values: heap, threads, classes, and the CPU usage from the second reading on. */
public class OverviewReaderTest {

    @Test
    public void readsTheHeapTheThreadsAndTheClasses() throws IOException {
        OverviewSample sample =
                new OverviewReader(new LocalJvmConnection()).read().orElseThrow();

        assertThat(sample.timeMillis()).isPositive();
        assertThat(sample.heap().getUsed()).isPositive();
        assertThat(sample.threads().live()).isPositive();
        assertThat(sample.threads().peak())
                .isGreaterThanOrEqualTo(sample.threads().live());
        assertThat(sample.threads().totalStarted())
                .isGreaterThanOrEqualTo(sample.threads().live());
        assertThat(sample.classes().loaded()).isPositive();
        assertThat(sample.classes().unloaded()).isNotNegative();
        assertThat(sample.cpuPercent()).isEmpty();
    }

    @Test
    public void theSecondReadingHasTheCpuUsage() throws IOException {
        OverviewReader reader = new OverviewReader(new LocalJvmConnection());
        reader.read();
        letUptimeMove();

        OverviewSample sample = reader.read().orElseThrow();

        assertThat(sample.cpuPercent().orElseThrow()).isBetween(0.0, CpuUsage.MAX_PERCENT);
    }

    @Test
    public void withoutTheProcessCpuTimeThereIsNoCpuUsage() throws IOException {
        OverviewReader reader = new OverviewReader(new LocalJvmConnection(ManagementFactory.getMemoryMXBean(), false));
        reader.read();
        letUptimeMove();

        assertThat(reader.read().orElseThrow().cpuPercent()).isEmpty();
    }

    @Test
    public void aTargetWithoutPlatformBeansHasNoReading() throws IOException {
        assertThat(new OverviewReader(new LocalJvmConnection(null, true)).read())
                .isEmpty();
    }

    /** Waits for the uptime, counted in milliseconds, to tick at least once. */
    static void letUptimeMove() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        long start = runtime.getUptime();
        while (runtime.getUptime() == start) {
            Thread.onSpinWait();
        }
    }
}
