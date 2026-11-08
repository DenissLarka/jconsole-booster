package com.druvu.jconsole.core.trust;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Trust-on-first-use {@link X509TrustManager} for a single JMXMP endpoint.
 *
 * <p>Trust resolution order for the server certificate:
 *
 * <ol>
 *   <li>a trusted authority (the JVM default trust store) signed it for this very host &rarr; accept silently;
 *   <li>otherwise, when a CA-validated certificate is required &rarr; refuse;
 *   <li>otherwise its SHA-256 is already pinned for this {@code host:port} &rarr; accept silently;
 *   <li>otherwise ask the operator via {@link CertTrustPrompt}: the louder question when another certificate is pinned
 *       for the endpoint, the other-host question when an authority signed it for other names, the first-use question
 *       otherwise (trust once / trust and remember / cancel).
 * </ol>
 *
 * <p>Used only on the TLS leg the profile selector chooses; a plaintext connection never touches it.
 */
public final class TofuTrustManager implements X509TrustManager {

    private static final X509TrustManager DEFAULT = defaultTrustManager();

    private final String hostPort;
    private final CertTrustPrompt prompt;
    private final TrustedCertStore store;
    private final boolean requireValidChain;
    private final X509TrustManager authorities;

    TofuTrustManager(String hostPort, CertTrustPrompt prompt, TrustedCertStore store, boolean requireValidChain) {
        this(hostPort, prompt, store, requireValidChain, DEFAULT);
    }

    /** @param authorities the certificate authorities trusted without asking: the JVM's own, a test's in tests */
    TofuTrustManager(
            String hostPort,
            CertTrustPrompt prompt,
            TrustedCertStore store,
            boolean requireValidChain,
            X509TrustManager authorities) {
        this.hostPort = hostPort;
        this.prompt = prompt;
        this.store = store;
        this.requireValidChain = requireValidChain;
        this.authorities = authorities;
    }

    /**
     * A TLS socket factory for one endpoint whose trust is this class's: the default trust store, then the pins in the
     * default {@link TrustedCertStore}, then {@code prompt}. Protocol and ciphers are left to the transport.
     */
    public static SSLSocketFactory socketFactory(String hostPort, CertTrustPrompt prompt, boolean requireValidChain)
            throws IOException {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(
                    null,
                    new TrustManager[] {
                        new TofuTrustManager(hostPort, prompt, TrustedCertStore.getDefault(), requireValidChain)
                    },
                    null);
            return context.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new IOException("TLS setup failed", e);
        }
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
        boolean signedByAuthority = signedByTrustedAuthority(chain, authType);
        // An authority's signature is enough only for the host it certifies: any public certificate chains to a
        // trusted authority, the one an interceptor holds for its own name too.
        if (signedByAuthority && CertifiedNames.cover(chain[0], CertifiedNames.hostOf(hostPort))) {
            return;
        }
        if (requireValidChain) {
            // The operator asked for strict PKI: no trust on first use, so anything an authority does not certify for
            // this very host is refused.
            throw new CertificateException("Server certificate for " + hostPort
                    + (signedByAuthority
                            ? " is issued for other hosts " + CertifiedNames.of(chain[0])
                            : " does not chain to a trusted CA")
                    + ", and a CA-validated certificate was required");
        }
        String fingerprint = sha256(chain[0]);
        if (store.isTrusted(hostPort, fingerprint)) {
            return;
        }
        // A non-empty pin set here means every pinned fingerprint differs from the one just presented (an equal one
        // would have returned above): the certificate has CHANGED — either a benign rotation (jmxmp's default
        // ephemeral cert is regenerated on each restart) or a man-in-the-middle. Route it to the louder prompt.
        Set<String> known = store.pinnedFingerprints(hostPort);
        boolean changed = !known.isEmpty();
        CertTrustPrompt.Decision decision;
        if (prompt == null) {
            decision = CertTrustPrompt.Decision.CANCEL;
        } else if (changed) {
            decision = prompt.confirmChanged(hostPort, chain[0], fingerprint, List.copyOf(known));
        } else if (signedByAuthority) {
            decision = prompt.confirmOtherHost(hostPort, chain[0], fingerprint, CertifiedNames.of(chain[0]));
        } else {
            decision = prompt.confirm(hostPort, chain[0], fingerprint);
        }
        switch (decision) {
            case TRUST_REMEMBER -> {
                // On the changed path, replace the superseded pin rather than accumulate a second trusted fingerprint.
                if (changed) {
                    store.replacePersistent(hostPort, fingerprint);
                } else {
                    store.rememberPersistent(hostPort, fingerprint);
                }
            }
            case TRUST_ONCE -> store.rememberForSession(hostPort, fingerprint);
            case CANCEL ->
                throw new CertificateException(
                        "Operator declined the server certificate for " + hostPort + " (SHA-256 " + fingerprint + ")");
        }
    }

    private boolean signedByTrustedAuthority(X509Certificate[] chain, String authType) {
        try {
            authorities.checkServerTrusted(chain, authType);
            return true;
        } catch (CertificateException notSignedByATrustedAuthority) {
            return false;
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return authorities.getAcceptedIssuers();
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
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot initialise the default trust manager", e);
        }
    }
}
