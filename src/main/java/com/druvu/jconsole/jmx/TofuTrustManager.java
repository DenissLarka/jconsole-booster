package com.druvu.jconsole.jmx;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Trust-on-first-use {@link X509TrustManager} for a single JMXMP endpoint.
 *
 * <p>Trust resolution order for the server certificate:
 *
 * <ol>
 *   <li>validates against the JVM default trust store (CA-signed) &rarr; accept silently;
 *   <li>otherwise its SHA-256 is already pinned for this {@code host:port} &rarr; accept silently;
 *   <li>otherwise ask the operator via {@link CertTrustPrompt} (trust once / trust and remember / cancel).
 * </ol>
 *
 * <p>Used only on the TLS leg the profile selector chooses; a plaintext connection never touches it.
 */
final class TofuTrustManager implements X509TrustManager {

    private static final X509TrustManager DEFAULT = defaultTrustManager();

    private final String hostPort;
    private final CertTrustPrompt prompt;
    private final TrustedCertStore store;

    TofuTrustManager(String hostPort, CertTrustPrompt prompt, TrustedCertStore store) {
        this.hostPort = hostPort;
        this.prompt = prompt;
        this.store = store;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // JCB is always the client; it never authenticates a peer the way a server would.
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("Server presented no certificate for " + hostPort);
        }
        try {
            DEFAULT.checkServerTrusted(chain, authType);
            return; // CA-signed / already trusted by the default trust store
        } catch (CertificateException notTrustedByDefault) {
            // fall through to trust-on-first-use
        }
        String fingerprint = sha256(chain[0]);
        if (store.isTrusted(hostPort, fingerprint)) {
            return;
        }
        CertTrustPrompt.Decision decision =
                (prompt == null) ? CertTrustPrompt.Decision.CANCEL : prompt.confirm(hostPort, chain[0], fingerprint);
        switch (decision) {
            case TRUST_REMEMBER -> store.rememberPersistent(hostPort, fingerprint);
            case TRUST_ONCE -> store.rememberForSession(hostPort, fingerprint);
            case CANCEL ->
                throw new CertificateException(
                        "Operator declined the server certificate for " + hostPort + " (SHA-256 " + fingerprint + ")");
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return DEFAULT.getAcceptedIssuers();
    }

    /** SHA-256 of the certificate's DER encoding, colon-separated uppercase hex. */
    static String sha256(X509Certificate cert) throws CertificateException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
            StringBuilder sb = new StringBuilder(digest.length * 3);
            for (byte b : digest) {
                if (sb.length() > 0) {
                    sb.append(':');
                }
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateException("SHA-256 not available", e);
        }
    }

    private static X509TrustManager defaultTrustManager() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509) {
                    return x509;
                }
            }
            throw new IllegalStateException("No X509TrustManager in the default trust manager set");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialise the default trust manager", e);
        }
    }
}
