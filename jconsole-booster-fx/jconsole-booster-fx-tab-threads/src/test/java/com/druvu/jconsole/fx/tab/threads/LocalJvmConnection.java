package com.druvu.jconsole.fx.tab.threads;

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
 * A connection to the test's own JVM: the real platform MXBeans read in process, with a thread bean of the test's
 * choosing. What the Threads tab does not read is empty.
 */
final class LocalJvmConnection implements JmxDataAccess {

    @Override
    public String getDisplayName() {
        return "test connection";
    }

    private final ThreadMXBean threads;
    private final boolean lockUsage;

    /** This JVM, reporting lock usage as a current JVM does. */
    LocalJvmConnection() {
        this(ManagementFactory.getThreadMXBean(), true);
    }

    /**
     * @param threads the thread bean; null for a target without platform MXBeans
     * @param lockUsage whether the target reports lock usage
     */
    LocalJvmConnection(ThreadMXBean threads, boolean lockUsage) {
        this.threads = threads;
        this.lockUsage = lockUsage;
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
        return threads != null;
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
        return lockUsage;
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
        return ManagementFactory.getMemoryMXBean();
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
        return threads;
    }

    @Override
    public com.sun.management.OperatingSystemMXBean getSunOperatingSystemMXBean() {
        return ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
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

    /** The search a target that reports lock usage runs: monitors and ownable synchronizers alike. */
    @Override
    public long[] findDeadlockedThreads() {
        return threads.findDeadlockedThreads();
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
