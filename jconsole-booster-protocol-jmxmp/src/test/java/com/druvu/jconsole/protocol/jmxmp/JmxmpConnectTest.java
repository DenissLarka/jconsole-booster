package com.druvu.jconsole.protocol.jmxmp;

import com.druvu.jconsole.core.connect.JMXConnectionManager;
import com.druvu.jconsole.core.trust.CertTrustPrompt;
import com.druvu.jconsole.core.trust.TrustedCertStore;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
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

/**
 * End to end over JMXMP: {@link JMXConnectionManager} with this module's transport (found through druvu-lib-loader)
 * against a self-signed, TLS-mandating server — trust on first use, a changed certificate, a required valid chain.
 */
public class JmxmpConnectTest {

    @Test
    public void autoNegotiatesTlsAndPromptsOnceForSelfSignedCert() throws IOException {
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
    public void changedCertificateRoutesToConfirmChanged() throws IOException {
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
    public void requireValidChainRejectsSelfSignedWithoutPrompting() throws IOException {
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
    public void declinedCertificateFailsTheConnection() throws IOException {
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
    private static JMXConnectorServer startSecuredTarget(int port) throws IOException {
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
