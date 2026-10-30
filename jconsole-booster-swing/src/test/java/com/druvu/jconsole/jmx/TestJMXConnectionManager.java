package com.druvu.jconsole.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
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

    // --- MirrorServerProfiles: transport auto-negotiation (no credentials → plaintext allowed) ---

    @Test
    public void selectorRequestsTlsSaslWhenServerOffersIt() throws Exception {
        Map<String, Object> env = new HashMap<>();
        new MirrorServerProfiles(false).selectProfiles(env, "TLS SASL/PLAIN");
        Assert.assertEquals(env.get("jmx.remote.profiles"), "TLS SASL/PLAIN");
    }

    @Test
    public void selectorRequestsNoProfilesForPlaintextServer() throws Exception {
        Map<String, Object> env = new HashMap<>();
        env.put("jmx.remote.profiles", "stale");
        new MirrorServerProfiles(false).selectProfiles(env, "");
        Assert.assertFalse(env.containsKey("jmx.remote.profiles"), "plaintext server → no client profiles");
    }

    @Test
    public void selectorIgnoresProfilesThisBuildDoesNotSpeak() throws Exception {
        Map<String, Object> env = new HashMap<>();
        new MirrorServerProfiles(false).selectProfiles(env, "SASL/CRAM-MD5 TLS SASL/PLAIN");
        Assert.assertEquals(env.get("jmx.remote.profiles"), "TLS SASL/PLAIN");
    }

    // --- MirrorServerProfiles: never send credentials over plaintext (hard block) ---

    @Test(expectedExceptions = PlaintextCredentialsRefusedException.class)
    public void credentialedPlaintextIsRefusedBeforeSasl() throws Exception {
        // Credentials present + server offers no encryption → the selector throws (before any SASL/PLAIN send).
        new MirrorServerProfiles(true).selectProfiles(new HashMap<>(), "");
    }

    @Test
    public void credentialedTlsIsAllowed() throws Exception {
        Map<String, Object> env = new HashMap<>();
        new MirrorServerProfiles(true).selectProfiles(env, "TLS SASL/PLAIN");
        Assert.assertEquals(env.get("jmx.remote.profiles"), "TLS SASL/PLAIN", "encrypted → credentials may flow");
    }

    @Test
    public void anonymousPlaintextIsAllowed() throws Exception {
        // No credentials → a legacy plaintext server is fine (only session-hijack risk, no secret to leak).
        Map<String, Object> env = new HashMap<>();
        new MirrorServerProfiles(false).selectProfiles(env, "");
        Assert.assertFalse(env.containsKey("jmx.remote.profiles"), "anonymous plaintext connects in the clear");
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

    @Test
    public void pinnedFingerprintsUnionsPersistentAndSession() throws IOException {
        Path tmp = Files.createTempFile("trusted-certs", ".txt");
        Files.deleteIfExists(tmp);
        try {
            TrustedCertStore store = new TrustedCertStore(tmp);
            store.rememberPersistent("host:7091", "AA:BB");
            store.rememberForSession("host:7091", "CC:DD");
            Set<String> pins = store.pinnedFingerprints("host:7091");
            Assert.assertEquals(pins.size(), 2);
            Assert.assertTrue(pins.contains("AABB"), "persistent pin present (canonical form)");
            Assert.assertTrue(pins.contains("CCDD"), "session pin present (canonical form)");
            Assert.assertTrue(store.pinnedFingerprints("unknown:1").isEmpty(), "unknown host → empty");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void replacePersistentSwapsPinAndPreservesOtherLines() throws IOException {
        Path tmp = Files.createTempFile("trusted-certs", ".txt");
        try {
            Files.write(tmp, List.of("# my trusted servers", "host:7091\tAA:AA:AA", "other:9000\tBB:BB:BB"));
            new TrustedCertStore(tmp).replacePersistent("host:7091", "CC:CC:CC");

            TrustedCertStore reloaded = new TrustedCertStore(tmp);
            Assert.assertTrue(reloaded.isTrusted("host:7091", "CC:CC:CC"), "new pin trusted after reload");
            Assert.assertFalse(reloaded.isTrusted("host:7091", "AA:AA:AA"), "superseded pin removed");
            Assert.assertTrue(reloaded.isTrusted("other:9000", "BB:BB:BB"), "unrelated host preserved");

            List<String> lines = Files.readAllLines(tmp);
            Assert.assertTrue(
                    lines.stream().anyMatch(l -> l.startsWith("# my trusted servers")), "comment line preserved");
            Assert.assertEquals(
                    lines.stream().filter(l -> l.trim().startsWith("host:7091")).count(),
                    1,
                    "exactly one pin line for the changed host (no accumulation)");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void replacePersistentClearsStaleSessionPin() throws IOException {
        Path tmp = Files.createTempFile("trusted-certs", ".txt");
        Files.deleteIfExists(tmp);
        try {
            TrustedCertStore store = new TrustedCertStore(tmp);
            store.rememberForSession("host:7091", "AA:AA");
            Assert.assertTrue(store.isTrusted("host:7091", "AA:AA"));
            store.replacePersistent("host:7091", "BB:BB");
            Assert.assertFalse(store.isTrusted("host:7091", "AA:AA"), "stale session pin cleared on replace");
            Assert.assertTrue(store.isTrusted("host:7091", "BB:BB"));
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

    @Test
    public void changedCertificateRoutesToConfirmChanged() throws Exception {
        int port = freePort();
        JMXConnectorServer server = startSecuredTarget(port);
        try {
            String hostPort = "localhost:" + port;
            // Pre-seed a DIFFERENT trusted fingerprint for this endpoint (the default store is what connect() uses),
            // so the real server's self-signed cert reads as "changed" rather than first-use.
            TrustedCertStore.getDefault().rememberForSession(hostPort, "00:11:22:33:44:55:66:77:88:99");

            AtomicInteger firstUse = new AtomicInteger();
            AtomicInteger changed = new AtomicInteger();
            CertTrustPrompt prompt = new CertTrustPrompt() {
                @Override
                public Decision confirm(String hp, X509Certificate c, String fp) {
                    firstUse.incrementAndGet();
                    return Decision.TRUST_ONCE;
                }

                @Override
                public Decision confirmChanged(String hp, X509Certificate c, String fp, List<String> previous) {
                    changed.incrementAndGet();
                    Assert.assertFalse(previous.isEmpty(), "the changed prompt must receive the previous pin(s)");
                    return Decision.TRUST_ONCE;
                }
            };
            JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://localhost:" + port);
            JMXConnectionManager.ConnectionResult result = JMXConnectionManager.connect(url, "admin", "admin", prompt);
            Assert.assertNotNull(result.connector());
            Assert.assertEquals(
                    changed.get(), 1, "a differing pinned cert must trigger the changed prompt exactly once");
            Assert.assertEquals(firstUse.get(), 0, "the first-use prompt must NOT fire when a pin already exists");
            result.connector().close();
        } finally {
            server.stop();
        }
    }

    @Test
    public void requireValidChainRejectsSelfSignedWithoutPrompting() throws Exception {
        int port = freePort();
        JMXConnectorServer server = startSecuredTarget(port);
        try {
            AtomicInteger prompts = new AtomicInteger();
            CertTrustPrompt trustingButCounts = (hostPort, cert, fingerprint) -> {
                prompts.incrementAndGet();
                return CertTrustPrompt.Decision.TRUST_ONCE;
            };
            JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://localhost:" + port);
            try {
                JMXConnectionManager.connect(url, "admin", "admin", trustingButCounts, true);
                Assert.fail("a self-signed cert must be rejected when a CA-validated chain is required");
            } catch (IOException expected) {
                // expected — the self-signed server cert does not chain to a trusted CA
            }
            Assert.assertEquals(prompts.get(), 0, "require-valid-chain must NOT fall through to the trust prompt");
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
