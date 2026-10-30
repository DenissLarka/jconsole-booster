package com.druvu.jconsole.fx.tab.mbeans;

import com.druvu.jconsole.core.vm.ConnectionState;
import com.druvu.jconsole.core.vm.ConnectionStateListener;
import com.druvu.jconsole.core.vm.JmxDataAccess;
import com.druvu.jconsole.core.vm.MemoryPoolProxy;
import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.Map;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * A connection for the tests over an in-JVM MBean server. The MBeans tab only uses the MBean server; everything else
 * refuses.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ServerDataAccess implements JmxDataAccess {

    @Override
    public String getDisplayName() {
        return "test connection";
    }

    private final MBeanServer server;

    ServerDataAccess(MBeanServer server) {
        this.server = server;
    }

    @Override
    public MBeanServer getMBeanServerConnection() {
        return server;
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
        return false;
    }

    @Override
    public boolean hasHotSpotDiagnosticMXBean() {
        return false;
    }

    @Override
    public boolean hasCompilationMXBean() {
        return false;
    }

    @Override
    public boolean isLockUsageSupported() {
        return false;
    }

    @Override
    public ClassLoadingMXBean getClassLoadingMXBean() {
        throw unused();
    }

    @Override
    public CompilationMXBean getCompilationMXBean() {
        throw unused();
    }

    @Override
    public MemoryMXBean getMemoryMXBean() {
        throw unused();
    }

    @Override
    public OperatingSystemMXBean getOperatingSystemMXBean() {
        throw unused();
    }

    @Override
    public RuntimeMXBean getRuntimeMXBean() {
        throw unused();
    }

    @Override
    public ThreadMXBean getThreadMXBean() {
        throw unused();
    }

    @Override
    public com.sun.management.OperatingSystemMXBean getSunOperatingSystemMXBean() {
        throw unused();
    }

    @Override
    public HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() {
        throw unused();
    }

    @Override
    public Collection<MemoryPoolProxy> getMemoryPoolProxies() {
        throw unused();
    }

    @Override
    public Collection<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        throw unused();
    }

    @Override
    public Map<ObjectName, MBeanInfo> getMBeans(String domain) {
        throw unused();
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) {
        throw unused();
    }

    @Override
    public long[] findDeadlockedThreads() {
        throw unused();
    }

    @Override
    public void markAsDead() {
        // the server lives as long as the test
    }

    @Override
    public void addConnectionStateListener(ConnectionStateListener listener) {
        // the state never changes
    }

    @Override
    public void removeConnectionStateListener(ConnectionStateListener listener) {
        // the state never changes
    }

    private static UnsupportedOperationException unused() {
        return new UnsupportedOperationException("The MBeans tab does not use it");
    }
}
