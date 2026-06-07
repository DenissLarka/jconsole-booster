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

import com.druvu.jmxmp.shared.ClientProfilePolicy;
import java.io.IOException;
import java.security.cert.X509Certificate;
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
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Encapsulates pure-JMX connection establishment logic extracted from {@link ProxyClient}. All methods are static; this
 * class is not instantiable.
 */
public final class JMXConnectionManager {

    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    /**
     * Test-only trust manager that accepts any server certificate. Used solely to reach a
     * druvu-lib-jmxmp server running with its on-the-fly self-signed certificate (no fixed trust
     * anchor to pin). Enabled only when {@code -Djcb.jmxmp.tls.trustall=true}; never used in normal
     * operator runs.
     */
    @SuppressWarnings("java:S4830") // intentional: trust-all for the self-signed integration target only
    private static final X509TrustManager TRUST_ALL = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // test-only: accept any client certificate
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // test-only: accept any server certificate (ephemeral self-signed target)
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

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
     * @param userName optional username (may be {@code null})
     * @param password optional password (may be {@code null})
     * @return a {@link ConnectionResult}
     * @throws IOException on failure
     */
    public static ConnectionResult connect(JMXServiceURL jmxUrl, String userName, String password) throws IOException {
        Map<String, Object> env = new HashMap<>();
        if (userName != null || password != null) {
            env.put(JMXConnector.CREDENTIALS, new String[] {userName, password});
        }
        // JCB is an operator-driven JMX client: it must reach whatever endpoint the operator
        // points it at — overwhelmingly plaintext production JMXMP. druvu-lib-jmxmp's client
        // gate defaults to mandatory TLS+SASL/PLAIN; unrestricted() restores connect-to-anything
        // behaviour (plaintext / TLS / SASL alike). The lib's JmxmpSerialFilter still enforces
        // default-deny deserialization regardless of transport, so this does not widen RCE surface.
        env.put(ClientProfilePolicy.ENV_KEY, ClientProfilePolicy.unrestricted());
        applyOptionalTlsProfile(env);
        JMXConnector jmxc = JMXConnectorFactory.connect(jmxUrl, env);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        return detectCapabilities(jmxc, mbsc);
    }

    /**
     * Optional TLS+SASL/PLAIN profile for the integration test against a druvu-lib-jmxmp server that
     * uses its on-the-fly self-signed certificate. When {@code -Djcb.jmxmp.tls.trustall=true} is set,
     * add the {@code "TLS SASL/PLAIN"} profile and a TLS socket factory that trusts any server
     * certificate (there is no fixed cert to pin against an ephemeral self-signed identity);
     * credentials flow from the connection dialog as usual. With the property unset (the common case)
     * this is a no-op and the connection proceeds under the unrestricted client policy.
     *
     * <p><b>Test only.</b> Trusting any certificate is not safe for real connections — it is enabled
     * solely to reach the bundled sample target in the GUI smoke run / integration test.
     */
    private static void applyOptionalTlsProfile(Map<String, Object> env) throws IOException {
        if (!Boolean.getBoolean("jcb.jmxmp.tls.trustall")) {
            return;
        }
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[] {TRUST_ALL}, null);
            env.put("jmx.remote.profiles", "TLS SASL/PLAIN");
            env.put("jmx.remote.tls.socket.factory", ctx.getSocketFactory());
        } catch (Exception e) {
            throw new IOException("TLS setup failed", e);
        }
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
