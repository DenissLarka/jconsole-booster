/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.druvu.jconsole.core.vm;

import com.druvu.jconsole.core.connect.JMXConnectionManager;
import com.druvu.jconsole.core.connect.JMXConnectionManager.ConnectionResult;
import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.text.Resources;
import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyClient implements JmxDataAccess {

    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;

    // Last exception thrown by connect(); surfaced by the retry dialog on an initial-connect failure.
    private volatile Exception lastConnectException;

    // Called on the thread that changes the state; a user interface hands the change to its own thread.
    private final List<ConnectionStateListener> stateListeners = new CopyOnWriteArrayList<>();

    private static final Map<String, ProxyClient> cache = Collections.synchronizedMap(new HashMap<>());

    // connect() runs on a worker thread while the tabs read from theirs: what it sets is volatile, and the platform
    // MXBean proxies, created on first use, are guarded by platformBeansLock.
    private final Object platformBeansLock = new Object();

    private volatile boolean isDead = true;
    private final String hostName;
    private final int port;
    private final String userName;
    private final String password;
    private volatile boolean hasPlatformMXBeans;
    private volatile boolean hasHotSpotDiagnosticMXBean;
    private volatile boolean hasCompilationMXBean;
    private volatile boolean supportsLockUsage;

    // REVISIT: VMPanel and other places relying using getUrl().

    private final String url;

    private final JMXServiceURL jmxUrl;
    private volatile MBeanServerConnection mbsc;
    private volatile SnapshotMBeanServerConnection server;
    private volatile JMXConnector jmxc;
    private final String connectionName;
    private final String displayName;
    private final boolean requireValidChain;

    private ClassLoadingMXBean classLoadingMBean = null;
    private CompilationMXBean compilationMBean = null;
    private MemoryMXBean memoryMBean = null;
    private OperatingSystemMXBean operatingSystemMBean = null;
    private RuntimeMXBean runtimeMBean = null;
    private ThreadMXBean threadMBean = null;

    private com.sun.management.OperatingSystemMXBean sunOperatingSystemMXBean = null;
    private HotSpotDiagnosticMXBean hotspotDiagnosticMXBean = null;

    private List<MemoryPoolProxy> memoryPoolProxies = null;
    private List<GarbageCollectorMXBean> garbageCollectorMBeans = null;

    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
    private static final ObjectName MEMORY_POOLS = pattern(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",*");
    private static final ObjectName GARBAGE_COLLECTORS =
            pattern(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");

    private static ObjectName pattern(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException("A platform MXBean name is not an ObjectName: " + name, e);
        }
    }

    private ProxyClient(String url, String userName, String password, boolean requireValidChain) throws IOException {
        this.url = url;
        this.connectionName = getConnectionName(url, userName);
        this.displayName = connectionName;
        this.requireValidChain = requireValidChain;
        this.jmxUrl = new JMXServiceURL(url);
        this.hostName = jmxUrl.getHost();
        this.port = jmxUrl.getPort();
        this.userName = userName;
        this.password = password;
    }

    private void setConnectionState(ConnectionState state) {
        ConnectionState oldState = this.connectionState;
        this.connectionState = state;
        if (oldState != state) {
            stateListeners.forEach(listener -> listener.connectionStateChanged(oldState, state));
        }
    }

    public ConnectionState getConnectionState() {
        return this.connectionState;
    }

    public void flush() {
        if (server != null) {
            server.flush();
        }
    }

    public void connect() {
        setConnectionState(ConnectionState.CONNECTING);
        try {
            lastConnectException = null;
            tryConnect();
            setConnectionState(ConnectionState.CONNECTED);
        } catch (Exception e) {
            lastConnectException = e;
            log.debug("Connection to {} failed", url, e);
            setConnectionState(ConnectionState.DISCONNECTED);
        }
    }

    private void tryConnect() throws IOException {
        final ConnectionResult result = JMXConnectionManager.connect(jmxUrl, userName, password, requireValidChain);
        this.jmxc = result.connector();
        this.mbsc = result.connection();
        this.server = Snapshot.newSnapshot(mbsc);
        this.hasPlatformMXBeans = result.hasPlatformMXBeans();
        this.hasHotSpotDiagnosticMXBean = result.hasHotSpotDiagnosticMXBean();
        this.hasCompilationMXBean = result.hasCompilationMXBean();
        this.supportsLockUsage = result.supportsLockUsage();
        this.isDead = false;

        if (hasPlatformMXBeans) {
            // WORKAROUND for bug 5056632
            // Check if the access role is correct by getting a RuntimeMXBean
            getRuntimeMXBean();
        }
    }

    /**
     * Gets a proxy client for a given JMXServiceURL. {@code requireValidChain} (reject non-CA-validated certificates
     * instead of trust-on-first-use) is part of the cache identity, so a strict request never reuses a lenient cached
     * client — the same url/user connected once leniently and once strictly yields two distinct clients.
     */
    public static ProxyClient getProxyClient(String url, String userName, String password, boolean requireValidChain)
            throws IOException {
        final String key = getCacheKey(url, userName, password, requireValidChain);
        ProxyClient proxyClient = cache.get(key);
        if (proxyClient == null) {
            proxyClient = new ProxyClient(url, userName, password, requireValidChain);
            cache.put(key, proxyClient);
        }
        return proxyClient;
    }

    public static String getConnectionName(String url, String userName) {
        if (userName != null && userName.length() > 0) {
            return userName + "@" + url;
        } else {
            return url;
        }
    }

    private static String getCacheKey(String url, String userName, String password, boolean requireValidChain) {
        return (url == null ? "" : url) + ":" + (userName == null ? "" : userName) + ":"
                + (password == null ? "" : password) + ":" + requireValidChain;
    }

    public String connectionName() {
        return connectionName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public String toString() {
        if (!isConnected()) {
            return Resources.format(Messages.CONNECTION_NAME__DISCONNECTED_, displayName);
        } else {
            return displayName;
        }
    }

    public MBeanServerConnection getMBeanServerConnection() {
        return mbsc;
    }

    public SnapshotMBeanServerConnection getSnapshotMBeanServerConnection() {
        return server;
    }

    public String getUrl() {
        return url;
    }

    /** The exception from the most recent failed {@link #connect()}, or {@code null} if the last attempt succeeded. */
    public Exception getLastConnectException() {
        return lastConnectException;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public void disconnect() {
        // Close MBeanServer connection
        if (jmxc != null) {
            try {
                jmxc.close();
            } catch (IOException e) {
                log.debug("Closing the connection to {} failed", url, e);
            }
        }
        // Reset platform MBean references: each one reads through the connection just closed
        synchronized (platformBeansLock) {
            classLoadingMBean = null;
            compilationMBean = null;
            memoryMBean = null;
            operatingSystemMBean = null;
            runtimeMBean = null;
            threadMBean = null;
            sunOperatingSystemMXBean = null;
            hotspotDiagnosticMXBean = null;
            memoryPoolProxies = null;
            garbageCollectorMBeans = null;
        }
        // Set connection state to DISCONNECTED
        if (!isDead) {
            isDead = true;
            setConnectionState(ConnectionState.DISCONNECTED);
        }
    }

    /**
     * Cleanly closes every cached client's JMX connector. Registered as a JVM shutdown hook (see {@code JConsole.main})
     * so quitting JCB — via the window close, the Exit menu, or a SIGTERM — closes each connection instead of letting
     * the socket drop, which the server would otherwise log as "client exited without closing connection". Best-effort:
     * a snapshot of the cache is closed outside the lock and per-client failures are ignored.
     */
    public static void disconnectAll() {
        List<ProxyClient> clients;
        synchronized (cache) {
            clients = new ArrayList<>(cache.values());
        }
        for (ProxyClient client : clients) {
            try {
                client.disconnect();
            } catch (Exception ignored) {
                // best-effort during shutdown
            }
        }
    }

    /** Returns the list of domains in which any MBean is currently registered. */
    public String[] getDomains() throws IOException {
        return server.getDomains();
    }

    /**
     * Returns a map of MBeans with ObjectName as the key and MBeanInfo value of a given domain. If domain is
     * {@code null}, all MBeans are returned. If no MBean is found, an empty map is returned.
     */
    public Map<ObjectName, MBeanInfo> getMBeans(String domain) throws IOException {

        ObjectName name = null;
        if (domain != null) {
            try {
                name = new ObjectName(domain + ":*");
            } catch (MalformedObjectNameException e) {
                // should not reach here
                assert false;
            }
        }
        Set<ObjectName> mbeans = server.queryNames(name, null);
        Map<ObjectName, MBeanInfo> result = HashMap.newHashMap(mbeans.size());
        Iterator<ObjectName> iterator = mbeans.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object instanceof ObjectName) {
                ObjectName o = (ObjectName) object;
                try {
                    MBeanInfo info = server.getMBeanInfo(o);
                    result.put(o, info);
                } catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
                    // the MBean left between the query and this read, or cannot describe itself: list the others
                    log.debug("No MBeanInfo for {}", o, e);
                }
            }
        }
        return result;
    }

    /** Returns a list of attributes of a named MBean. */
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws IOException {
        AttributeList list = null;
        try {
            list = server.getAttributes(name, attributes);
        } catch (InstanceNotFoundException | ReflectionException e) {
            // the MBean was unregistered, or refused the read: the caller treats null as "nothing to show"
            log.debug("Reading attributes of {} failed", name, e);
        }
        return list;
    }

    /** Set the value of a specific attribute of a named MBean. */
    public void setAttribute(ObjectName name, Attribute attribute)
            throws InvalidAttributeValueException, MBeanException, IOException {
        try {
            server.setAttribute(name, attribute);
        } catch (InstanceNotFoundException | AttributeNotFoundException | ReflectionException e) {
            log.warn("Setting {} on {} failed: {}", attribute.getName(), name, e.toString());
        }
    }

    /**
     * Invokes an operation of a named MBean.
     *
     * @throws MBeanException Wraps an exception thrown by the MBean's invoked method.
     */
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws IOException, MBeanException {
        Object result = null;
        try {
            result = server.invoke(name, operationName, params, signature);
        } catch (InstanceNotFoundException | ReflectionException e) {
            log.warn("Invoking {} on {} failed: {}", operationName, name, e.toString());
        }
        return result;
    }

    public ClassLoadingMXBean getClassLoadingMXBean() throws IOException {
        synchronized (platformBeansLock) {
            if (hasPlatformMXBeans && classLoadingMBean == null) {
                classLoadingMBean = ManagementFactory.newPlatformMXBeanProxy(
                        server, ManagementFactory.CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class);
            }
            return classLoadingMBean;
        }
    }

    public CompilationMXBean getCompilationMXBean() throws IOException {
        synchronized (platformBeansLock) {
            if (hasCompilationMXBean && compilationMBean == null) {
                compilationMBean = ManagementFactory.newPlatformMXBeanProxy(
                        server, ManagementFactory.COMPILATION_MXBEAN_NAME, CompilationMXBean.class);
            }
            return compilationMBean;
        }
    }

    public Collection<MemoryPoolProxy> getMemoryPoolProxies() throws IOException {
        synchronized (platformBeansLock) {
            // Built once per connection: a pool that appears later is not shown.
            if (memoryPoolProxies == null) {
                List<MemoryPoolProxy> pools = new ArrayList<>();
                for (ObjectName poolName : server.queryNames(MEMORY_POOLS, null)) {
                    pools.add(new MemoryPoolProxy(this, poolName));
                }
                memoryPoolProxies = List.copyOf(pools);
            }
            return memoryPoolProxies;
        }
    }

    public Collection<GarbageCollectorMXBean> getGarbageCollectorMXBeans() throws IOException {
        synchronized (platformBeansLock) {
            // Built once per connection: a collector that appears later is not shown.
            if (garbageCollectorMBeans == null) {
                List<GarbageCollectorMXBean> collectors = new ArrayList<>();
                for (ObjectName collector : server.queryNames(GARBAGE_COLLECTORS, null)) {
                    String name = ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",name="
                            + collector.getKeyProperty("name");
                    collectors.add(
                            ManagementFactory.newPlatformMXBeanProxy(server, name, GarbageCollectorMXBean.class));
                }
                garbageCollectorMBeans = List.copyOf(collectors);
            }
            return garbageCollectorMBeans;
        }
    }

    public MemoryMXBean getMemoryMXBean() throws IOException {
        synchronized (platformBeansLock) {
            if (hasPlatformMXBeans && memoryMBean == null) {
                memoryMBean = ManagementFactory.newPlatformMXBeanProxy(
                        server, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
            }
            return memoryMBean;
        }
    }

    public RuntimeMXBean getRuntimeMXBean() throws IOException {
        synchronized (platformBeansLock) {
            if (hasPlatformMXBeans && runtimeMBean == null) {
                runtimeMBean = ManagementFactory.newPlatformMXBeanProxy(
                        server, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
            }
            return runtimeMBean;
        }
    }

    public ThreadMXBean getThreadMXBean() throws IOException {
        synchronized (platformBeansLock) {
            if (hasPlatformMXBeans && threadMBean == null) {
                threadMBean = ManagementFactory.newPlatformMXBeanProxy(
                        server, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
            }
            return threadMBean;
        }
    }

    public OperatingSystemMXBean getOperatingSystemMXBean() throws IOException {
        synchronized (platformBeansLock) {
            if (hasPlatformMXBeans && operatingSystemMBean == null) {
                operatingSystemMBean = ManagementFactory.newPlatformMXBeanProxy(
                        server, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
            }
            return operatingSystemMBean;
        }
    }

    public com.sun.management.OperatingSystemMXBean getSunOperatingSystemMXBean() throws IOException {
        synchronized (platformBeansLock) {
            try {
                ObjectName on = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
                if (sunOperatingSystemMXBean == null
                        && server.isInstanceOf(on, "com.sun.management.OperatingSystemMXBean")) {
                    sunOperatingSystemMXBean = ManagementFactory.newPlatformMXBeanProxy(
                            server,
                            ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
                            com.sun.management.OperatingSystemMXBean.class);
                }
            } catch (InstanceNotFoundException e) {
                return null;
            } catch (MalformedObjectNameException e) {
                return null; // should never reach here
            }
            return sunOperatingSystemMXBean;
        }
    }

    public HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() throws IOException {
        synchronized (platformBeansLock) {
            if (hasHotSpotDiagnosticMXBean && hotspotDiagnosticMXBean == null) {
                hotspotDiagnosticMXBean = ManagementFactory.newPlatformMXBeanProxy(
                        server, HOTSPOT_DIAGNOSTIC_MXBEAN_NAME, HotSpotDiagnosticMXBean.class);
            }
            return hotspotDiagnosticMXBean;
        }
    }

    public <T> T getMXBean(ObjectName objName, Class<T> interfaceClass) throws IOException {
        return ManagementFactory.newPlatformMXBeanProxy(server, objName.toString(), interfaceClass);
    }

    // Return thread IDs of deadlocked threads or null if any.
    // It finds deadlocks involving only monitors if it's a Tiger VM.
    // Otherwise, it finds deadlocks involving both monitors and
    // the concurrent locks.
    public long[] findDeadlockedThreads() throws IOException {
        ThreadMXBean tm = getThreadMXBean();
        if (supportsLockUsage && tm.isSynchronizerUsageSupported()) {
            return tm.findDeadlockedThreads();
        } else {
            return tm.findMonitorDeadlockedThreads();
        }
    }

    public void markAsDead() {
        disconnect();
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean isConnected() {
        return !isDead();
    }

    public boolean hasPlatformMXBeans() {
        return this.hasPlatformMXBeans;
    }

    public boolean hasHotSpotDiagnosticMXBean() {
        return this.hasHotSpotDiagnosticMXBean;
    }

    public boolean hasCompilationMXBean() {
        return this.hasCompilationMXBean;
    }

    public boolean isLockUsageSupported() {
        return supportsLockUsage;
    }

    public boolean isRegistered(ObjectName name) throws IOException {
        return server.isRegistered(name);
    }

    @Override
    public void addConnectionStateListener(ConnectionStateListener listener) {
        stateListeners.add(listener);
    }

    @Override
    public void removeConnectionStateListener(ConnectionStateListener listener) {
        stateListeners.remove(listener);
    }

    //
    // Snapshot MBeanServerConnection:
    //
    // This is an object that wraps an existing MBeanServerConnection and adds
    // caching to it, as follows:
    //
    // - The first time an attribute is called in a given MBean, the result is
    // cached. Every subsequent time getAttribute is called for that attribute,
    // the cached result is returned.
    //
    // - Before every call to VMPanel.update() or when the Refresh button in the
    // Attributes table is pressed down, the attribute cache is flushed. Then
    // any subsequent call to getAttribute will retrieve all the values for
    // the attributes that are known to the cache.
    //
    // - The attribute cache uses a learning approach, and only the attributes
    // that are in the cache will be retrieved between two updates in a row.
    //

    public interface SnapshotMBeanServerConnection extends MBeanServerConnection {
        /** Flush all cached values of attributes. */
        void flush();
    }

    public static class Snapshot {
        private Snapshot() {}

        public static SnapshotMBeanServerConnection newSnapshot(MBeanServerConnection mbsc) {
            final InvocationHandler ih = new SnapshotInvocationHandler(mbsc);
            return (SnapshotMBeanServerConnection) Proxy.newProxyInstance(
                    Snapshot.class.getClassLoader(), new Class<?>[] {SnapshotMBeanServerConnection.class}, ih);
        }
    }

    static class SnapshotInvocationHandler implements InvocationHandler {

        private final MBeanServerConnection conn;
        private Map<ObjectName, Map<String, Object>> cachedValues = new HashMap<>();
        private Map<ObjectName, Set<String>> cachedNames = new HashMap<>();

        SnapshotInvocationHandler(MBeanServerConnection conn) {
            this.conn = conn;
        }

        synchronized void flush() {
            cachedValues = new HashMap<>();
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final String methodName = method.getName();
            if (methodName.equals("getAttribute")) {
                return getAttribute((ObjectName) args[0], (String) args[1]);
            } else if (methodName.equals("getAttributes")) {
                return getAttributes((ObjectName) args[0], (String[]) args[1]);
            } else if (methodName.equals("flush")) {
                flush();
                return null;
            } else {
                try {
                    return method.invoke(conn, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        private Object getAttribute(ObjectName objName, String attrName)
                throws MBeanException, InstanceNotFoundException, AttributeNotFoundException, ReflectionException,
                        IOException {
            final Map<String, Object> values = getCachedAttributes(objName, Collections.singleton(attrName));
            Object value = values.get(attrName);
            if (value != null || values.containsKey(attrName)) {
                return value;
            }
            // Not in cache, presumably because it was omitted from the
            // getAttributes result because of an exception. The following
            // call will probably provoke the same exception.
            return conn.getAttribute(objName, attrName);
        }

        private AttributeList getAttributes(ObjectName objName, String[] attrNames)
                throws InstanceNotFoundException, ReflectionException, IOException {
            final Map<String, Object> values = getCachedAttributes(objName, new TreeSet<>(Arrays.asList(attrNames)));
            final AttributeList list = new AttributeList();
            for (String attrName : attrNames) {
                final Object value = values.get(attrName);
                if (value != null || values.containsKey(attrName)) {
                    list.add(new Attribute(attrName, value));
                }
            }
            return list;
        }

        private synchronized Map<String, Object> getCachedAttributes(ObjectName objName, Set<String> attrNames)
                throws InstanceNotFoundException, ReflectionException, IOException {
            Map<String, Object> values = cachedValues.get(objName);
            if (values != null && values.keySet().containsAll(attrNames)) {
                return values;
            }
            attrNames = new TreeSet<>(attrNames);
            Set<String> oldNames = cachedNames.get(objName);
            if (oldNames != null) {
                attrNames.addAll(oldNames);
            }
            values = new HashMap<>();
            final AttributeList attrs = conn.getAttributes(objName, attrNames.toArray(new String[attrNames.size()]));
            for (Attribute attr : attrs.asList()) {
                values.put(attr.getName(), attr.getValue());
            }
            cachedValues.put(objName, values);
            cachedNames.put(objName, attrNames);
            return values;
        }
    }
}
