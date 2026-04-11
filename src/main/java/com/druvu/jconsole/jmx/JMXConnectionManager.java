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

/**
 * Encapsulates pure-JMX connection establishment logic extracted from {@link ProxyClient}. All methods are static; this
 * class is not instantiable.
 */
public final class JMXConnectionManager {

    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    // -----------------------------------------------------------------------
    // Public result records
    // -----------------------------------------------------------------------

    /**
     * The result of a successful JMX connection attempt.
     *
     * @param connector the {@link JMXConnector} that was opened
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

    // -----------------------------------------------------------------------
    // Private constructor — not instantiable
    // -----------------------------------------------------------------------

    private JMXConnectionManager() {}

    // -----------------------------------------------------------------------
    // Connection methods
    // -----------------------------------------------------------------------

    /**
     * Establishes a JMX connection to the given service URL using the supplied credentials.
     *
     * @param jmxUrl the JMX service URL to connect to
     * @param userName optional user name (may be {@code null})
     * @param password optional password (may be {@code null})
     * @return a {@link ConnectionResult}
     * @throws IOException on failure
     */
    public static ConnectionResult connect(JMXServiceURL jmxUrl, String userName, String password) throws IOException {
        Map<String, Object> env = new HashMap<>();
        if (userName != null || password != null) {
            env.put(JMXConnector.CREDENTIALS, new String[] {userName, password});
        }
        JMXConnector jmxc = JMXConnectorFactory.connect(jmxUrl, env);
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
