package com.druvu.jconsole.fx.tab.overview;

import com.druvu.jconsole.core.vm.ConnectionState;
import com.druvu.jconsole.core.vm.ConnectionStateListener;
import com.druvu.jconsole.core.vm.JmxDataAccess;
import com.druvu.jconsole.core.vm.MemoryPoolProxy;
import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * A connection to the test's own JVM: the real platform MXBeans read in process, with a memory bean of the test's
 * choosing and the HotSpot view of the operating system on or off. What the Overview does not read is empty.
 */
final class LocalJvmConnection implements JmxDataAccess {

    @Override
    public String getDisplayName() {
        return "test connection";
    }

    private final MemoryMXBean memory;
    private final boolean processCpuTime;

    /** This JVM as it is. */
    LocalJvmConnection() {
        this(ManagementFactory.getMemoryMXBean(), true);
    }

    /**
     * @param memory the memory bean; null for a target without platform MXBeans
     * @param processCpuTime whether the target has the HotSpot view of the operating system, which tells the CPU time
     */
    LocalJvmConnection(MemoryMXBean memory, boolean processCpuTime) {
        this.memory = memory;
        this.processCpuTime = processCpuTime;
    }

    @Override
    public ConnectionState getConnectionState() {
        return ConnectionState.CONNECTED;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public boolean hasPlatformMXBeans() {
        return memory != null;
    }

    @Override
    public boolean hasHotSpotDiagnosticMXBean() {
        return true;
    }

    @Override
    public boolean hasCompilationMXBean() {
        return true;
    }

    @Override
    public boolean isLockUsageSupported() {
        return true;
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    @Override
    public ClassLoadingMXBean getClassLoadingMXBean() {
        return ManagementFactory.getClassLoadingMXBean();
    }

    @Override
    public CompilationMXBean getCompilationMXBean() {
        return ManagementFactory.getCompilationMXBean();
    }

    @Override
    public MemoryMXBean getMemoryMXBean() {
        return memory;
    }

    @Override
    public OperatingSystemMXBean getOperatingSystemMXBean() {
        return ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public RuntimeMXBean getRuntimeMXBean() {
        return ManagementFactory.getRuntimeMXBean();
    }

    @Override
    public ThreadMXBean getThreadMXBean() {
        return ManagementFactory.getThreadMXBean();
    }

    @Override
    public com.sun.management.OperatingSystemMXBean getSunOperatingSystemMXBean() {
        return processCpuTime
                ? ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class)
                : null;
    }

    @Override
    public HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() {
        return ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
    }

    @Override
    public Collection<MemoryPoolProxy> getMemoryPoolProxies() {
        return List.of();
    }

    @Override
    public Collection<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return ManagementFactory.getGarbageCollectorMXBeans();
    }

    @Override
    public Map<ObjectName, MBeanInfo> getMBeans(String domain) {
        return Map.of();
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) {
        return new AttributeList();
    }

    @Override
    public long[] findDeadlockedThreads() {
        return new long[0];
    }

    @Override
    public void markAsDead() {
        // a local JVM cannot be lost
    }

    @Override
    public void addConnectionStateListener(ConnectionStateListener listener) {
        // the state never changes
    }

    @Override
    public void removeConnectionStateListener(ConnectionStateListener listener) {
        // the state never changes
    }
}
