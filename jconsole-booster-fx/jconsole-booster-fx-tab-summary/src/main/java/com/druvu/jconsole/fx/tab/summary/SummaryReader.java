package com.druvu.jconsole.fx.tab.summary;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.text.Resources;
import com.druvu.jconsole.core.vm.JmxDataAccess;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Reads the summary from the target JVM's platform MXBeans. Every call goes over the connection, so it runs on the
 * update thread, never on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class SummaryReader {

    private static final DateTimeFormatter READ_AT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    private final JmxDataAccess connection;
    private final String connectionName;

    /**
     * @param connection the live connection to the target JVM
     * @param connectionName how the user knows the connection, shown on the page
     */
    SummaryReader(JmxDataAccess connection, String connectionName) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.connectionName = Objects.requireNonNull(connectionName, "connectionName");
    }

    /**
     * Reads the whole summary.
     *
     * @return the summary, or empty when the target does not expose the platform MXBeans
     * @throws IOException the connection is gone
     */
    Optional<VmSummary> read() throws IOException {
        if (!connection.hasPlatformMXBeans()) {
            return Optional.empty();
        }
        RuntimeMXBean runtime = connection.getRuntimeMXBean();
        OperatingSystemMXBean system = connection.getOperatingSystemMXBean();
        // null when the target is not a HotSpot-family JVM: its rows are then left out
        com.sun.management.OperatingSystemMXBean systemDetails = connection.getSunOperatingSystemMXBean();

        List<SummarySection> sections = List.of(
                virtualMachine(runtime, connection.getCompilationMXBean(), systemDetails),
                threads(connection.getThreadMXBean()),
                classes(connection.getClassLoadingMXBean()),
                memory(connection.getMemoryMXBean()),
                collectors(connection.getGarbageCollectorMXBeans()),
                operatingSystem(system, systemDetails));
        List<SummaryRow> paths = paths(runtime, SummaryFormat.pathSeparatorOf(system.getName()));
        return Optional.of(new VmSummary(READ_AT.format(LocalDateTime.now()), sections, paths));
    }

    private SummarySection virtualMachine(
            RuntimeMXBean runtime,
            CompilationMXBean compilation,
            com.sun.management.OperatingSystemMXBean systemDetails) {
        List<SummaryRow> rows = new ArrayList<>();
        rows.add(new SummaryRow(Messages.CONNECTION_NAME, connectionName));
        rows.add(new SummaryRow(
                Messages.VIRTUAL_MACHINE,
                Resources.format(Messages.SUMMARY_TAB_VM_VERSION, runtime.getVmName(), runtime.getVmVersion())));
        rows.add(new SummaryRow(Messages.VENDOR, runtime.getVmVendor()));
        rows.add(new SummaryRow(Messages.NAME, runtime.getName()));
        rows.add(new SummaryRow(Messages.UPTIME, SummaryFormat.millis(runtime.getUptime())));
        if (systemDetails != null) {
            rows.add(new SummaryRow(Messages.PROCESS_CPU_TIME, SummaryFormat.nanos(systemDetails.getProcessCpuTime())));
        }
        if (compilation == null) {
            rows.add(new SummaryRow(Messages.JIT_COMPILER, Messages.UNAVAILABLE));
        } else {
            rows.add(new SummaryRow(Messages.JIT_COMPILER, compilation.getName()));
            rows.add(new SummaryRow(
                    Messages.TOTAL_COMPILE_TIME,
                    compilation.isCompilationTimeMonitoringSupported()
                            ? SummaryFormat.millis(compilation.getTotalCompilationTime())
                            : Messages.UNAVAILABLE));
        }
        return new SummarySection("Virtual machine", rows);
    }

    private static SummarySection threads(ThreadMXBean threads) {
        return new SummarySection(
                "Threads",
                List.of(
                        new SummaryRow(Messages.LIVE_THREADS, SummaryFormat.count(threads.getThreadCount())),
                        new SummaryRow(Messages.PEAK, SummaryFormat.count(threads.getPeakThreadCount())),
                        new SummaryRow(Messages.DAEMON_THREADS, SummaryFormat.count(threads.getDaemonThreadCount())),
                        new SummaryRow(
                                Messages.TOTAL_THREADS_STARTED,
                                SummaryFormat.count(threads.getTotalStartedThreadCount()))));
    }

    private static SummarySection classes(ClassLoadingMXBean classes) {
        return new SummarySection(
                "Classes",
                List.of(
                        new SummaryRow(
                                Messages.CURRENT_CLASSES_LOADED, SummaryFormat.count(classes.getLoadedClassCount())),
                        new SummaryRow(
                                Messages.TOTAL_CLASSES_LOADED, SummaryFormat.count(classes.getTotalLoadedClassCount())),
                        new SummaryRow(
                                Messages.TOTAL_CLASSES_UNLOADED,
                                SummaryFormat.count(classes.getUnloadedClassCount()))));
    }

    private static SummarySection memory(MemoryMXBean memory) {
        MemoryUsage heap = memory.getHeapMemoryUsage();
        return new SummarySection(
                "Memory",
                List.of(
                        new SummaryRow(Messages.CURRENT_HEAP_SIZE, SummaryFormat.kbytes(heap.getUsed())),
                        new SummaryRow(Messages.MAXIMUM_HEAP_SIZE, SummaryFormat.kbytes(heap.getMax())),
                        new SummaryRow(Messages.COMMITTED_MEMORY, SummaryFormat.kbytes(heap.getCommitted())),
                        new SummaryRow(
                                Messages.SUMMARY_TAB_PENDING_FINALIZATION_LABEL,
                                Resources.format(
                                        Messages.SUMMARY_TAB_PENDING_FINALIZATION_VALUE,
                                        pendingFinalization(memory)))));
    }

    // Deprecated with finalization itself; a target on an older Java still finalizes, and JConsole shows the count.
    @SuppressWarnings("deprecation")
    private static int pendingFinalization(MemoryMXBean memory) {
        return memory.getObjectPendingFinalizationCount();
    }

    private static SummarySection collectors(Collection<GarbageCollectorMXBean> collectors) {
        List<SummaryRow> rows = new ArrayList<>();
        for (GarbageCollectorMXBean collector : collectors) {
            rows.add(new SummaryRow(
                    collector.getName(),
                    SummaryFormat.collections(collector.getCollectionCount(), collector.getCollectionTime())));
        }
        return new SummarySection("Garbage collectors", rows);
    }

    private static SummarySection operatingSystem(
            OperatingSystemMXBean system, com.sun.management.OperatingSystemMXBean systemDetails) {
        List<SummaryRow> rows = new ArrayList<>();
        rows.add(new SummaryRow(Messages.OPERATING_SYSTEM, system.getName() + " " + system.getVersion()));
        rows.add(new SummaryRow(Messages.ARCHITECTURE, system.getArch()));
        rows.add(new SummaryRow(Messages.NUMBER_OF_PROCESSORS, SummaryFormat.count(system.getAvailableProcessors())));
        if (systemDetails != null) {
            rows.add(new SummaryRow(
                    Messages.COMMITTED_VIRTUAL_MEMORY,
                    SummaryFormat.kbytes(systemDetails.getCommittedVirtualMemorySize())));
            rows.add(new SummaryRow(Messages.TOTAL_PHYSICAL_MEMORY, SummaryFormat.kbytes(totalMemory(systemDetails))));
            rows.add(new SummaryRow(Messages.FREE_PHYSICAL_MEMORY, SummaryFormat.kbytes(freeMemory(systemDetails))));
            rows.add(new SummaryRow(
                    Messages.TOTAL_SWAP_SPACE, SummaryFormat.kbytes(systemDetails.getTotalSwapSpaceSize())));
            rows.add(new SummaryRow(
                    Messages.FREE_SWAP_SPACE, SummaryFormat.kbytes(systemDetails.getFreeSwapSpaceSize())));
        }
        return new SummarySection("Operating system", rows);
    }

    private static List<SummaryRow> paths(RuntimeMXBean runtime, String separator) {
        String bootClassPath = runtime.isBootClassPathSupported()
                ? SummaryFormat.pathEntries(runtime.getBootClassPath(), separator)
                : Messages.UNAVAILABLE;
        return List.of(
                new SummaryRow(Messages.VM_ARGUMENTS, SummaryFormat.arguments(runtime.getInputArguments())),
                new SummaryRow(Messages.CLASS_PATH, SummaryFormat.pathEntries(runtime.getClassPath(), separator)),
                new SummaryRow(Messages.LIBRARY_PATH, SummaryFormat.pathEntries(runtime.getLibraryPath(), separator)),
                new SummaryRow(Messages.BOOT_CLASS_PATH, bootClassPath));
    }

    // A target older than Java 14 only has the physical-memory getters, deprecated since (JDK-8255934).
    @SuppressWarnings("deprecation")
    private static long totalMemory(com.sun.management.OperatingSystemMXBean systemDetails) {
        return firstAvailable(systemDetails::getTotalMemorySize, systemDetails::getTotalPhysicalMemorySize);
    }

    @SuppressWarnings("deprecation")
    private static long freeMemory(com.sun.management.OperatingSystemMXBean systemDetails) {
        return firstAvailable(systemDetails::getFreeMemorySize, systemDetails::getFreePhysicalMemorySize);
    }

    /**
     * The first getter the target answers. An attribute the target does not have comes back as an undeclared exception
     * from the MXBean proxy; a lost connection comes back the same way, with an {@link IOException} inside, and goes on
     * to the caller.
     */
    private static long firstAvailable(LongSupplier... getters) {
        for (LongSupplier getter : getters) {
            try {
                return getter.getAsLong();
            } catch (UndeclaredThrowableException notAnswered) {
                if (notAnswered.getCause() instanceof IOException) {
                    throw notAnswered;
                }
            }
        }
        return -1;
    }
}
