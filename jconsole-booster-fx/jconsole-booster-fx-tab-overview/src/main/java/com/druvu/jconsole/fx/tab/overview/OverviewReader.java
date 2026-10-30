package com.druvu.jconsole.fx.tab.overview;

import com.druvu.jconsole.core.vm.JmxDataAccess;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Reads the values the Overview charts from the target JVM, on the update thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class OverviewReader {

    /** The live connection; reading through it is this class's job. */
    private final JmxDataAccess connection;

    private final CpuUsage cpuUsage = new CpuUsage();

    OverviewReader(JmxDataAccess connection) {
        this.connection = connection;
    }

    /** A reading of the target JVM now, or empty when it has no platform MXBeans. */
    Optional<OverviewSample> read() throws IOException {
        MemoryMXBean memory = connection.getMemoryMXBean();
        ThreadMXBean threads = connection.getThreadMXBean();
        ClassLoadingMXBean classes = connection.getClassLoadingMXBean();
        if (memory == null || threads == null || classes == null) {
            return Optional.empty();
        }
        long timeMillis = System.currentTimeMillis();
        ThreadCounts threadCounts = new ThreadCounts(
                threads.getThreadCount(), threads.getPeakThreadCount(), threads.getTotalStartedThreadCount());
        ClassCounts classCounts = new ClassCounts(classes.getLoadedClassCount(), classes.getTotalLoadedClassCount());
        return Optional.of(
                new OverviewSample(timeMillis, memory.getHeapMemoryUsage(), threadCounts, classCounts, readCpuUsage()));
    }

    private OptionalDouble readCpuUsage() throws IOException {
        com.sun.management.OperatingSystemMXBean system = connection.getSunOperatingSystemMXBean();
        RuntimeMXBean runtime = connection.getRuntimeMXBean();
        if (system == null || runtime == null) {
            // only the HotSpot view of the operating system tells the process's CPU time
            return OptionalDouble.empty();
        }
        return cpuUsage.next(system.getProcessCpuTime(), runtime.getUptime(), system.getAvailableProcessors());
    }
}
