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
import java.security.GeneralSecurityException;
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
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Encapsulates pure-JMX connection establishment logic extracted from {@link ProxyClient}. All methods are static; this
 * class is not instantiable.
 */
@Slf4j
public final class JMXConnectionManager {

    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    /**
     * Fail-closed default trust prompt: with no UI installed, an untrusted (e.g. self-signed) server certificate is
     * declined rather than silently accepted. {@code JConsole} installs the interactive trust-on-first-use dialog at
     * launch via {@link #setCertTrustPrompt(CertTrustPrompt)}.
     */
    private static final CertTrustPrompt DENY = (hostPort, cert, fingerprint) -> CertTrustPrompt.Decision.CANCEL;

    private static volatile CertTrustPrompt promptProvider = DENY;

    /**
     * No-op default: a headless caller gets the exception; {@code JConsole} installs a dialog that explains the block.
     */
    private static volatile PlaintextCredentialAlert plaintextAlert = hostPort -> {};

    /**
     * Installs the UI shown when a connection is hard-refused for trying to send credentials over plaintext (see
     * {@link PlaintextCredentialsRefusedException}). Call once at startup; {@code null} restores the no-op default.
     */
    public static void setPlaintextCredentialAlert(PlaintextCredentialAlert alert) {
        plaintextAlert = (alert == null) ? hostPort -> {} : alert;
    }

    /**
     * Installs the operator-facing prompt asked to approve a server certificate that does not validate against the
     * default trust store. Call once at startup (see {@code JConsole.main}); {@code null} restores the fail-closed
     * default.
     */
    public static void setCertTrustPrompt(CertTrustPrompt prompt) {
        promptProvider = (prompt == null) ? DENY : prompt;
    }

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
     * Establishes a JMX connection to the given service URL using the trust prompt installed via
     * {@link #setCertTrustPrompt(CertTrustPrompt)}.
     */
    public static ConnectionResult connect(JMXServiceURL jmxUrl, String userName, String password) throws IOException {
        return connect(jmxUrl, userName, password, promptProvider, false);
    }

    /**
     * Establishes a JMX connection using the installed trust prompt. When {@code requireValidChain} is {@code true}, a
     * TLS server certificate that does not chain to a trusted CA is rejected outright rather than offered for
     * trust-on-first-use.
     */
    public static ConnectionResult connect(
            JMXServiceURL jmxUrl, String userName, String password, boolean requireValidChain) throws IOException {
        return connect(jmxUrl, userName, password, promptProvider, requireValidChain);
    }

    /**
     * Establishes a JMX connection, auto-negotiating transport with the server.
     *
     * <p>JCB keeps the client policy {@link ClientProfilePolicy#unrestricted()} and installs a
     * {@link MirrorServerProfiles} selector, so one connection reaches both legacy plaintext servers and hardened
     * druvu-lib-jmxmp 2.0.0 servers (mandatory TLS+SASL/PLAIN): the jmxmp handshake advertises the server's profiles
     * before the client commits, and the selector mirrors them in-band — no "use TLS" toggle, no retry. On the TLS leg,
     * a certificate that does not validate against the default trust store triggers {@code trustPrompt}
     * (trust-on-first-use). The lib's {@code JmxmpSerialFilter} still enforces default-deny deserialization regardless
     * of transport, so this does not widen RCE surface.
     *
     * @param jmxUrl the JMX service URL to connect to
     * @param userName optional username (may be {@code null})
     * @param password optional password (may be {@code null})
     * @param trustPrompt asked to approve an untrusted TLS server certificate; {@code null} declines all
     * @return a {@link ConnectionResult}
     * @throws IOException on failure (including an operator-declined certificate)
     */
    public static ConnectionResult connect(
            JMXServiceURL jmxUrl, String userName, String password, CertTrustPrompt trustPrompt) throws IOException {
        return connect(jmxUrl, userName, password, trustPrompt, false);
    }

    /**
     * As {@link #connect(JMXServiceURL, String, String, CertTrustPrompt)}, but with {@code requireValidChain}: when
     * {@code true}, an untrusted (e.g. self-signed) TLS certificate is rejected instead of being offered for
     * trust-on-first-use.
     */
    public static ConnectionResult connect(
            JMXServiceURL jmxUrl,
            String userName,
            String password,
            CertTrustPrompt trustPrompt,
            boolean requireValidChain)
            throws IOException {
        Map<String, Object> env = new HashMap<>();
        if (userName != null || password != null) {
            env.put(JMXConnector.CREDENTIALS, new String[] {userName, password});
        }
        // Unrestricted so the selector below is free to pick either transport; the default
        // mandatory-TLS client policy would reject a plaintext server before it runs.
        env.put(ClientProfilePolicy.ENV_KEY, ClientProfilePolicy.unrestricted());
        boolean credentialsPresent = userName != null || password != null;
        env.put("com.sun.jmx.remote.profile.selector", new MirrorServerProfiles(credentialsPresent));
        env.put("jmx.remote.tls.socket.factory", tofuSocketFactory(hostPort(jmxUrl), trustPrompt, requireValidChain));
        JMXConnector jmxc;
        try {
            jmxc = JMXConnectorFactory.connect(jmxUrl, env);
        } catch (IOException e) {
            // The selector hard-refuses a credentialed connection to a server that offered no encryption, throwing
            // before the password reaches the wire. jmxmp may wrap that exception, so scan the cause chain.
            PlaintextCredentialsRefusedException refused = plaintextCredentialRefusal(e);
            if (refused != null) {
                plaintextAlert.refused(hostPort(jmxUrl));
                throw refused;
            }
            throw e;
        }
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        return detectCapabilities(jmxc, mbsc);
    }

    /**
     * Walks {@code t}'s cause chain for the plaintext-credential refusal, or {@code null} if it is a different failure.
     */
    private static PlaintextCredentialsRefusedException plaintextCredentialRefusal(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof PlaintextCredentialsRefusedException refused) {
                return refused;
            }
        }
        return null;
    }

    private static String hostPort(JMXServiceURL jmxUrl) {
        return jmxUrl.getHost() + ":" + jmxUrl.getPort();
    }

    /**
     * SSL socket factory used only if the profile selector chooses TLS: trust is a {@link TofuTrustManager} keyed to
     * this endpoint (default trust store &rarr; pinned fingerprint &rarr; operator prompt). Protocol/ciphers are set by
     * druvu-lib-jmxmp's {@code TLSClientHandler} (TLS 1.3 by default).
     */
    private static SSLSocketFactory tofuSocketFactory(
            String hostPort, CertTrustPrompt trustPrompt, boolean requireValidChain) throws IOException {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(
                    null,
                    new TrustManager[] {
                        new TofuTrustManager(hostPort, trustPrompt, TrustedCertStore.getDefault(), requireValidChain)
                    },
                    null);
            return ctx.getSocketFactory();
        } catch (GeneralSecurityException e) {
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
