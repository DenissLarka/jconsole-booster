package com.druvu.jconsole.core.trust;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * The trust decision for a server certificate, against a test authority. The fixtures were made with {@code keytool}
 * (RSA 2048, valid to 2126): {@code ca.pem} the authority; {@code jmx-example-com.pem} signed by it for
 * {@code jmx.example.com}, {@code *.apps.example.com} and {@code 10.1.2.3}; {@code other-example-org.pem} signed by it
 * for {@code other.example.org}; {@code self-signed.pem} for {@code jmx.example.com}, signed by nobody.
 */
public class TofuTrustManagerTest {

    private static final String HOST_PORT = "jmx.example.com:7091";
    private static final String AUTH_TYPE = "ECDHE_RSA";

    private X509Certificate authority;
    private X509TrustManager authorities;
    private TrustedCertStore store;
    private RecordingPrompt prompt;

    @BeforeMethod
    public void setUp() throws IOException, GeneralSecurityException {
        authority = certificate("ca.pem");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("test-ca", authority);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        authorities = (X509TrustManager) factory.getTrustManagers()[0];
        Path file = Files.createTempFile("trusted-certs-", ".txt");
        file.toFile().deleteOnExit();
        store = new TrustedCertStore(file);
        prompt = new RecordingPrompt(CertTrustPrompt.Decision.TRUST_REMEMBER);
    }

    @Test
    public void authoritySignedForTheHostPassesWithoutAQuestion() throws IOException, CertificateException {
        manager(HOST_PORT, false).checkServerTrusted(chain("jmx-example-com.pem"), AUTH_TYPE);

        assertThat(prompt.questions).isEmpty();
    }

    @Test
    public void authoritySignedForAnotherHostIsAskedAbout() throws IOException, CertificateException {
        manager(HOST_PORT, false).checkServerTrusted(chain("other-example-org.pem"), AUTH_TYPE);

        assertThat(prompt.questions).containsExactly("other host");
        assertThat(prompt.certifiedNames).containsExactly("other.example.org");
    }

    @Test
    public void authoritySignedForTheAddressPassesWithoutAQuestion() throws IOException, CertificateException {
        manager("10.1.2.3:7091", false).checkServerTrusted(chain("jmx-example-com.pem"), AUTH_TYPE);

        assertThat(prompt.questions).isEmpty();
    }

    @Test
    public void authoritySignedForAWildcardPassesForOneLabel() throws IOException, CertificateException {
        manager("db1.apps.example.com:7091", false).checkServerTrusted(chain("jmx-example-com.pem"), AUTH_TYPE);

        assertThat(prompt.questions).isEmpty();
    }

    @Test
    public void authoritySignedForAnotherHostIsRefusedWhenAValidCertificateIsRequired()
            throws IOException, CertificateException {
        X509Certificate[] chain = chain("other-example-org.pem");

        assertThatThrownBy(() -> manager(HOST_PORT, true).checkServerTrusted(chain, AUTH_TYPE))
                .isInstanceOf(CertificateException.class);
        assertThat(prompt.questions).isEmpty();
    }

    /** The man in the middle with a real certificate for its own name meets the louder question at a pinned host. */
    @Test
    public void authoritySignedForAnotherHostAtAPinnedHostIsTheChangedQuestion()
            throws IOException, CertificateException {
        manager(HOST_PORT, false).checkServerTrusted(chain("self-signed.pem"), AUTH_TYPE);
        prompt.questions.clear();

        manager(HOST_PORT, false).checkServerTrusted(chain("other-example-org.pem"), AUTH_TYPE);

        assertThat(prompt.questions).containsExactly("changed");
    }

    /** A company certificate for another name, once remembered, passes from then on: the warning comes once. */
    @Test
    public void authoritySignedForAnotherHostOnceRememberedPassesQuietly() throws IOException, CertificateException {
        manager(HOST_PORT, false).checkServerTrusted(chain("other-example-org.pem"), AUTH_TYPE);
        prompt.questions.clear();

        manager(HOST_PORT, false).checkServerTrusted(chain("other-example-org.pem"), AUTH_TYPE);

        assertThat(prompt.questions).isEmpty();
    }

    @Test
    public void selfSignedIsTheFirstUseQuestion() throws IOException, CertificateException {
        manager(HOST_PORT, false).checkServerTrusted(chain("self-signed.pem"), AUTH_TYPE);

        assertThat(prompt.questions).containsExactly("first use");
    }

    @Test
    public void cancelRefusesTheCertificate() throws IOException, CertificateException {
        prompt = new RecordingPrompt(CertTrustPrompt.Decision.CANCEL);
        X509Certificate[] chain = chain("other-example-org.pem");

        assertThatThrownBy(() -> manager(HOST_PORT, false).checkServerTrusted(chain, AUTH_TYPE))
                .isInstanceOf(CertificateException.class);
    }

    private TofuTrustManager manager(String hostPort, boolean requireValidChain) {
        return new TofuTrustManager(hostPort, prompt, store, requireValidChain, authorities);
    }

    private X509Certificate[] chain(String leaf) throws IOException, CertificateException {
        X509Certificate certificate = certificate(leaf);
        boolean selfSigned = certificate.getIssuerX500Principal().equals(certificate.getSubjectX500Principal());
        return selfSigned ? new X509Certificate[] {certificate} : new X509Certificate[] {certificate, authority};
    }

    private static X509Certificate certificate(String name) throws IOException, CertificateException {
        try (InputStream in = TofuTrustManagerTest.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("missing fixture " + name);
            }
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    /** Answers every question the same way and records which one was asked. */
    private static final class RecordingPrompt implements CertTrustPrompt {

        private final Decision answer;
        private final List<String> questions = new ArrayList<>();
        private final List<String> certifiedNames = new ArrayList<>();

        RecordingPrompt(Decision answer) {
            this.answer = answer;
        }

        @Override
        public Decision confirm(String hostPort, X509Certificate cert, String sha256Fingerprint) {
            questions.add("first use");
            return answer;
        }

        @Override
        public Decision confirmChanged(
                String hostPort, X509Certificate cert, String sha256Fingerprint, List<String> previousFingerprints) {
            questions.add("changed");
            return answer;
        }

        @Override
        public Decision confirmOtherHost(
                String hostPort, X509Certificate cert, String sha256Fingerprint, List<String> names) {
            questions.add("other host");
            certifiedNames.addAll(names);
            return answer;
        }
    }
}
