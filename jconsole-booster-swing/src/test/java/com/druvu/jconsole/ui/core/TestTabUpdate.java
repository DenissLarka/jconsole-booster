package com.druvu.jconsole.ui.core;

import com.druvu.jconsole.jmx.MemoryPoolProxy;
import com.druvu.jconsole.jmx.api.JmxDataAccess;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.tools.jconsole.JConsoleContext;
import java.awt.GraphicsEnvironment;
import java.beans.PropertyChangeListener;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.swing.SwingWorker;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Verifies that {@link Tab} talks to {@link JmxDataAccess} (the interface) rather than to {@code VMPanel} or
 * {@code ProxyClient}. The mock data access counts every call so we can prove the abstraction is honored.
 */
public class TestTabUpdate {

    @BeforeClass
    public void skipIfHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new SkipException("headless environment — Tab extends JPanel and needs AWT");
        }
    }

    /** Counts the calls a Tab makes through the {@link JmxDataAccess} interface. */
    private static final class CountingDataAccess implements JmxDataAccess {
        boolean platformMxBeansAvailable = true;
        final AtomicInteger hasPlatformCalls = new AtomicInteger();

        @Override
        public JConsoleContext.ConnectionState getConnectionState() {
            return JConsoleContext.ConnectionState.CONNECTED;
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
            hasPlatformCalls.incrementAndGet();
            return platformMxBeansAvailable;
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
        public MBeanServerConnection getMBeanServerConnection() {
            return null;
        }

        @Override
        public ClassLoadingMXBean getClassLoadingMXBean() {
            return null;
        }

        @Override
        public CompilationMXBean getCompilationMXBean() {
            return null;
        }

        @Override
        public MemoryMXBean getMemoryMXBean() {
            return null;
        }

        @Override
        public OperatingSystemMXBean getOperatingSystemMXBean() {
            return null;
        }

        @Override
        public RuntimeMXBean getRuntimeMXBean() {
            return null;
        }

        @Override
        public ThreadMXBean getThreadMXBean() {
            return null;
        }

        @Override
        public com.sun.management.OperatingSystemMXBean getSunOperatingSystemMXBean() {
            return null;
        }

        @Override
        public HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() {
            return null;
        }

        @Override
        public Collection<MemoryPoolProxy> getMemoryPoolProxies() {
            return Collections.emptyList();
        }

        @Override
        public Collection<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
            return Collections.emptyList();
        }

        @Override
        public Map<ObjectName, MBeanInfo> getMBeans(String domain) {
            return Collections.emptyMap();
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
        public void markAsDead() {}

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {}

        @Override
        public void addWeakPropertyChangeListener(PropertyChangeListener l) {}

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    /** Minimal Tab subclass: returns a no-op SwingWorker so we can observe scheduling. */
    private static final class FakeTab extends Tab {
        final AtomicInteger workerRequests = new AtomicInteger();

        FakeTab(JmxDataAccess dataAccess) {
            super(null, dataAccess, "fake");
        }

        @Override
        public SwingWorker<?, ?> newSwingWorker() {
            workerRequests.incrementAndGet();
            return new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    return null;
                }
            };
        }
    }

    @Test
    public void updateConsultsDataAccessForPlatformMxBeans() {
        CountingDataAccess data = new CountingDataAccess();
        FakeTab tab = new FakeTab(data);

        tab.update();

        Assert.assertEquals(data.hasPlatformCalls.get(), 1, "Tab.update must consult JmxDataAccess.hasPlatformMXBeans");
        Assert.assertEquals(tab.workerRequests.get(), 1, "Tab.update must request a SwingWorker on the happy path");
    }

    @Test
    public void updateThrowsWhenPlatformMxBeansAreMissing() {
        CountingDataAccess data = new CountingDataAccess();
        data.platformMxBeansAvailable = false;
        FakeTab tab = new FakeTab(data);

        try {
            tab.update();
            Assert.fail("Expected UnsupportedOperationException when platform MXBeans are absent");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        Assert.assertEquals(data.hasPlatformCalls.get(), 1);
        Assert.assertEquals(tab.workerRequests.get(), 0, "no worker should be scheduled when MXBeans are missing");
    }

    @Test
    public void tabExposesItsDataAccessReference() {
        CountingDataAccess data = new CountingDataAccess();
        FakeTab tab = new FakeTab(data);
        // The Tab base class stores `dataAccess` as a protected final field — verify it is the same instance, i.e.
        // the constructor wired the interface through (and not, say, fetched it from VMPanel).
        Assert.assertSame(tab.dataAccess, data);
    }
}
