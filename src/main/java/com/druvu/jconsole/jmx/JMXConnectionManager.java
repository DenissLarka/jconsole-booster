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

package com.druvu.jconsole.jmx;

import static java.lang.management.ManagementFactory.COMPILATION_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.THREAD_MXBEAN_NAME;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.rmi.server.RemoteRef;
import java.util.HashMap;
import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import sun.rmi.server.UnicastRef2;
import sun.rmi.transport.LiveRef;

/**
 * Encapsulates pure-JMX connection establishment logic extracted from {@link ProxyClient}. All methods are static; this
 * class is not instantiable.
 */
public final class JMXConnectionManager {

    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    private static final SslRMIClientSocketFactory sslRMIClientSocketFactory = new SslRMIClientSocketFactory();

    // -----------------------------------------------------------------------
    // RMI stub class infrastructure
    // -----------------------------------------------------------------------

    private static final String rmiServerImplStubClassName = "javax.management.remote.rmi.RMIServerImpl_Stub";
    static final Class<? extends Remote> rmiServerImplStubClass;

    static {
        // FIXME: RMIServerImpl_Stub is generated at build time
        // after jconsole is built. We need to investigate if
        // the Makefile can be fixed to build jconsole in the
        // right order. As a workaround for now, we dynamically
        // load RMIServerImpl_Stub class instead of statically
        // referencing it.
        Class<? extends Remote> serverStubClass = null;
        try {
            serverStubClass = Class.forName(rmiServerImplStubClassName).asSubclass(Remote.class);
        } catch (ClassNotFoundException e) {
            // should never reach here
            throw new InternalError(e.getMessage(), e);
        }
        rmiServerImplStubClass = serverStubClass;
    }

    // -----------------------------------------------------------------------
    // Public result records
    // -----------------------------------------------------------------------

    /**
     * The result of a successful JMX connection attempt.
     *
     * @param connector the {@link JMXConnector} that was opened, or {@code null} for the self-monitor path
     * @param connection the raw {@link MBeanServerConnection}
     * @param hasPlatformMXBeans whether standard platform MXBeans are registered
     * @param hasHotSpotDiagnosticMXBean whether the HotSpot diagnostic MXBean is present
     * @param hasCompilationMXBean whether the compilation MXBean is present
     * @param supportsLockUsage whether {@code findDeadlockedThreads} is available
     */
    public record ConnectionResult(
            JMXConnector connector,
            MBeanServerConnection connection,
            boolean hasPlatformMXBeans,
            boolean hasHotSpotDiagnosticMXBean,
            boolean hasCompilationMXBean,
            boolean supportsLockUsage) {}

    /**
     * SSL-detection result from {@link #checkSslConfig}.
     *
     * @param stub the {@link RMIServer} stub looked up from the registry
     * @param sslRegistry whether the RMI registry itself was reached via SSL
     * @param sslStub whether the retrieved stub uses an SSL socket factory
     */
    public record SslInfo(RMIServer stub, boolean sslRegistry, boolean sslStub) {}

    // -----------------------------------------------------------------------
    // Private constructor — not instantiable
    // -----------------------------------------------------------------------

    private JMXConnectionManager() {}

    // -----------------------------------------------------------------------
    // Static stub check
    // -----------------------------------------------------------------------

    static void checkStub(Remote stub, Class<? extends Remote> stubClass) {
        // Check remote stub is from the expected class.
        //
        if (stub.getClass() != stubClass) {
            if (!Proxy.isProxyClass(stub.getClass())) {
                throw new SecurityException("Expecting a " + stubClass.getName() + " stub!");
            } else {
                InvocationHandler handler = Proxy.getInvocationHandler(stub);
                if (handler.getClass() != RemoteObjectInvocationHandler.class) {
                    throw new SecurityException("Expecting a dynamic proxy instance with a "
                            + RemoteObjectInvocationHandler.class.getName() + " invocation handler!");
                } else {
                    stub = (Remote) handler;
                }
            }
        }
        // Check RemoteRef in stub is from the expected class
        // "sun.rmi.server.UnicastRef2".
        //
        RemoteRef ref = ((RemoteObject) stub).getRef();
        if (ref.getClass() != UnicastRef2.class) {
            throw new SecurityException("Expecting a " + UnicastRef2.class.getName() + " remote reference in stub!");
        }
        // Check RMIClientSocketFactory in stub is from the expected class
        // "javax.rmi.ssl.SslRMIClientSocketFactory".
        //
        LiveRef liveRef = ((UnicastRef2) ref).getLiveRef();
        RMIClientSocketFactory csf = liveRef.getClientSocketFactory();
        if (csf == null || csf.getClass() != SslRMIClientSocketFactory.class) {
            throw new SecurityException(
                    "Expecting a " + SslRMIClientSocketFactory.class.getName() + " RMI client socket factory in stub!");
        }
    }

    // -----------------------------------------------------------------------
    // SSL config detection
    // -----------------------------------------------------------------------

    /**
     * Detects whether the RMI registry and the {@code jmxrmi} stub use SSL.
     *
     * @param registryHostName hostname of the RMI registry
     * @param registryPort port of the RMI registry
     * @return an {@link SslInfo} record describing what was found
     * @throws IOException if the registry cannot be reached or {@code jmxrmi} is not bound
     */
    public static SslInfo checkSslConfig(String registryHostName, int registryPort) throws IOException {
        // Get the reference to the RMI Registry and lookup RMIServer stub
        //
        Registry registry;
        RMIServer stub;
        boolean sslRegistry;
        try {
            registry = LocateRegistry.getRegistry(registryHostName, registryPort, sslRMIClientSocketFactory);
            try {
                stub = (RMIServer) registry.lookup("jmxrmi");
            } catch (NotBoundException nbe) {
                throw (IOException) new IOException(nbe.getMessage()).initCause(nbe);
            }
            sslRegistry = true;
        } catch (IOException e) {
            registry = LocateRegistry.getRegistry(registryHostName, registryPort);
            try {
                stub = (RMIServer) registry.lookup("jmxrmi");
            } catch (NotBoundException nbe) {
                throw (IOException) new IOException(nbe.getMessage()).initCause(nbe);
            }
            sslRegistry = false;
        }
        // Perform the checks for secure stub
        //
        boolean sslStub;
        try {
            checkStub(stub, rmiServerImplStubClass);
            sslStub = true;
        } catch (SecurityException e) {
            sslStub = false;
        }
        return new SslInfo(stub, sslRegistry, sslStub);
    }

    // -----------------------------------------------------------------------
    // Connection methods
    // -----------------------------------------------------------------------

    /**
     * Establishes a connection for the self-monitor path (localhost:0).
     *
     * @return a {@link ConnectionResult} with a {@code null} connector
     * @throws IOException on failure
     */
    public static ConnectionResult connectSelf() throws IOException {
        MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();
        return detectCapabilities(null, mbsc);
    }

    /**
     * Establishes a connection for a local VM attach path.
     *
     * @param lvm the local virtual machine to connect to
     * @return a {@link ConnectionResult}
     * @throws IOException on failure
     */
    public static ConnectionResult connect(LocalVirtualMachine lvm) throws IOException {
        if (!lvm.isManageable()) {
            lvm.startManagementAgent();
            if (!lvm.isManageable()) {
                throw new IOException(lvm + " not manageable");
            }
        }
        JMXServiceURL jmxUrl = new JMXServiceURL(lvm.connectorAddress());
        Map<String, Object> env = new HashMap<>();
        JMXConnector jmxc = JMXConnectorFactory.connect(jmxUrl, env);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        return detectCapabilities(jmxc, mbsc);
    }

    /**
     * Establishes a connection for the URL / credential / VM-connector path.
     *
     * @param jmxUrl the JMX service URL to connect to
     * @param userName optional user name (maybe {@code null})
     * @param password optional password (maybe {@code null})
     * @param isVmConnector {@code true} if this is a VM connector (RMI registry path)
     * @param existingStub pre-fetched {@link RMIServer} stub, or {@code null} to fetch via SSL-config detection
     * @param registryHostName hostname of the RMI registry (used only when {@code isVmConnector && existingStub == *
     *     null})
     * @param registryPort port of the RMI registry (same condition)
     * @param requireSSL if {@code true}, passes {@code jmx.remote.x.check.stub=true}
     * @return a {@link ConnectionResult}
     * @throws IOException on failure
     */
    public static ConnectionResult connect(
            JMXServiceURL jmxUrl,
            String userName,
            String password,
            boolean isVmConnector,
            RMIServer existingStub,
            String registryHostName,
            int registryPort,
            boolean requireSSL)
            throws IOException {

        Map<String, Object> env = new HashMap<>();
        if (requireSSL) {
            env.put("jmx.remote.x.check.stub", "true");
        }
        if (userName != null || password != null) {
            env.put(JMXConnector.CREDENTIALS, new String[] {userName, password});
        }

        JMXConnector jmxc;
        if (isVmConnector) {
            RMIServer stub = existingStub;
            if (stub == null) {
                SslInfo sslInfo = checkSslConfig(registryHostName, registryPort);
                stub = sslInfo.stub();
            }
            jmxc = new RMIConnector(stub, null);
            jmxc.connect(env);
        } else {
            jmxc = JMXConnectorFactory.connect(jmxUrl, env);
        }

        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        return detectCapabilities(jmxc, mbsc);
    }

    // -----------------------------------------------------------------------
    // Capability detection (private helper)
    // -----------------------------------------------------------------------

    private static ConnectionResult detectCapabilities(JMXConnector jmxc, MBeanServerConnection mbsc)
            throws IOException {
        boolean hasPlatformMXBeans;
        boolean hasHotSpotDiagnosticMXBean;
        boolean hasCompilationMXBean = false;
        boolean supportsLockUsage = false;

        try {
            ObjectName threadOn = new ObjectName(THREAD_MXBEAN_NAME);
            hasPlatformMXBeans = mbsc.isRegistered(threadOn);
            hasHotSpotDiagnosticMXBean = mbsc.isRegistered(new ObjectName(HOTSPOT_DIAGNOSTIC_MXBEAN_NAME));

            if (hasPlatformMXBeans) {
                MBeanOperationInfo[] mopis = mbsc.getMBeanInfo(threadOn).getOperations();
                for (MBeanOperationInfo op : mopis) {
                    if (op.getName().equals("findDeadlockedThreads")) {
                        supportsLockUsage = true;
                        break;
                    }
                }
                ObjectName compilationOn = new ObjectName(COMPILATION_MXBEAN_NAME);
                hasCompilationMXBean = mbsc.isRegistered(compilationOn);
            }
        } catch (MalformedObjectNameException e) {
            throw new InternalError(e.getMessage());
        } catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
            throw new InternalError(e.getMessage(), e);
        }

        return new ConnectionResult(
                jmxc, mbsc, hasPlatformMXBeans, hasHotSpotDiagnosticMXBean, hasCompilationMXBean, supportsLockUsage);
    }
}
