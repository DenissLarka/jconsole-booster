package com.druvu.jconsole.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanServer;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests for {@link JMXConnectionManager} and the TLS trust-on-first-use path it drives. */
public class TestJMXConnectionManager {

    @Test
    public void connectionResultRecordExposesAccessors() {
        // Smoke check the public ConnectionResult contract — building one with nulls should not throw, and accessors
        // should round-trip the boolean flags. This guards against an accidental record-component reorder.
        JMXConnectionManager.ConnectionResult r =
                new JMXConnectionManager.ConnectionResult(null, null, true, false, true, false);
        Assert.assertNull(r.connector());
        Assert.assertNull(r.connection());
        Assert.assertTrue(r.hasPlatformMXBeans());
        Assert.assertFalse(r.hasHotSpotDiagnosticMXBean());
        Assert.assertTrue(r.hasCompilationMXBean());
        Assert.assertFalse(r.supportsLockUsage());
    }

    // --- MirrorServerProfiles: transport auto-negotiation ---

    @Test
    public void selectorRequestsTlsSaslWhenServerOffersIt() {
        Map<String, Object> env = new HashMap<>();
        new MirrorServerProfiles().selectProfiles(env, "TLS SASL/PLAIN");
        Assert.assertEquals(env.get("jmx.remote.profiles"), "TLS SASL/PLAIN");
    }

    @Test
    public void selectorRequestsNoProfilesForPlaintextServer() {
        Map<String, Object> env = new HashMap<>();
        env.put("jmx.remote.profiles", "stale");
        new MirrorServerProfiles().selectProfiles(env, "");
        Assert.assertFalse(env.containsKey("jmx.remote.profiles"), "plaintext server → no client profiles");
    }

    @Test
    public void selectorIgnoresProfilesThisBuildDoesNotSpeak() {
        Map<String, Object> env = new HashMap<>();
        new MirrorServerProfiles().selectProfiles(env, "SASL/CRAM-MD5 TLS SASL/PLAIN");
        Assert.assertEquals(env.get("jmx.remote.profiles"), "TLS SASL/PLAIN");
    }

    // --- TrustedCertStore: persistence vs session ---

    @Test
    public void persistentPinSurvivesReloadAndIsColonInsensitive() throws IOException {
        Path tmp = Files.createTempFile("trusted-certs", ".txt");
        Files.deleteIfExists(tmp);
        try {
            TrustedCertStore store = new TrustedCertStore(tmp);
            Assert.assertFalse(store.isTrusted("host:7091", "AA:BB:CC"));
            store.rememberPersistent("host:7091", "AA:BB:CC");
            Assert.assertTrue(new TrustedCertStore(tmp).isTrusted("host:7091", "aabbcc"));
            Assert.assertFalse(new TrustedCertStore(tmp).isTrusted("host:7091", "DD:EE:FF"));
            Assert.assertFalse(new TrustedCertStore(tmp).isTrusted("other:7091", "AA:BB:CC"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void sessionPinIsNotPersisted() throws IOException {
        Path tmp = Files.createTempFile("trusted-certs", ".txt");
        Files.deleteIfExists(tmp);
        try {
            TrustedCertStore store = new TrustedCertStore(tmp);
            store.rememberForSession("host:7091", "AA:BB:CC");
            Assert.assertTrue(store.isTrusted("host:7091", "AA:BB:CC"));
            Assert.assertFalse(new TrustedCertStore(tmp).isTrusted("host:7091", "AA:BB:CC"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // --- end-to-end: the regression fix — connect to a self-signed, TLS-mandating jmxmp server ---

    @Test
    public void autoNegotiatesTlsAndPromptsOnceForSelfSignedCert() throws Exception {
        int port = freePort();
        JMXConnectorServer server = startSecuredTarget(port);
        try {
            AtomicInteger prompts = new AtomicInteger();
            CertTrustPrompt acceptOnce = (hostPort, cert, fingerprint) -> {
                prompts.incrementAndGet();
                return CertTrustPrompt.Decision.TRUST_ONCE;
            };
            JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://localhost:" + port);
            JMXConnectionManager.ConnectionResult result =
                    JMXConnectionManager.connect(url, "admin", "admin", acceptOnce);
            Assert.assertNotNull(result.connector());
            Assert.assertTrue(
                    result.hasPlatformMXBeans(), "platform MXBeans must be reachable over the TLS connection");
            Assert.assertEquals(prompts.get(), 1, "a self-signed cert must trigger exactly one trust prompt");
            result.connector().close();
        } finally {
            server.stop();
        }
    }

    @Test(expectedExceptions = IOException.class)
    public void declinedCertificateFailsTheConnection() throws Exception {
        int port = freePort();
        JMXConnectorServer server = startSecuredTarget(port);
        try {
            CertTrustPrompt decline = (hostPort, cert, fingerprint) -> CertTrustPrompt.Decision.CANCEL;
            JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://localhost:" + port);
            JMXConnectionManager.connect(url, "admin", "admin", decline);
        } finally {
            server.stop();
        }
    }

    /**
     * A druvu-lib-jmxmp 2.0.0 secured target: profile defaults to TLS SASL/PLAIN with an ephemeral self-signed cert.
     */
    private static JMXConnectorServer startSecuredTarget(int port) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        JMXAuthenticator authenticator = credentials -> {
            if (!(credentials instanceof String[] c)
                    || c.length != 2
                    || !"admin".equals(c[0])
                    || !"admin".equals(c[1])) {
                throw new SecurityException("Invalid credentials (expected admin/admin)");
            }
            return new Subject(true, Set.of(new JMXPrincipal(c[0])), Set.of(), Set.of());
        };
        Map<String, Object> env = new HashMap<>();
        env.put(JMXConnectorServer.AUTHENTICATOR, authenticator);
        JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://localhost:" + port);
        JMXConnectorServer server = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        server.start();
        return server;
    }

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
