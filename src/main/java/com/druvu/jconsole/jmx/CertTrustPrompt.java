package com.druvu.jconsole.jmx;

import java.security.cert.X509Certificate;

/**
 * Callback the TLS trust layer uses to ask the operator whether to trust a server certificate that does not validate
 * against the default trust store (typically a self-signed cert — the druvu-lib-jmxmp 2.0.0 default).
 * Trust-on-first-use, SSH style.
 *
 * <p>Implementations may be invoked off the Swing EDT (connections are established on a worker thread); a UI
 * implementation must marshal to the EDT itself (see {@code CertTrustDialog}).
 */
public interface CertTrustPrompt {

    /** The operator's decision for an untrusted server certificate. */
    enum Decision {
        /** Do not trust; abort the connection. */
        CANCEL,
        /** Trust for this connection only; do not remember. */
        TRUST_ONCE,
        /** Trust and remember (pin) for future connections to this host. */
        TRUST_REMEMBER
    }

    /**
     * @param hostPort the target endpoint ({@code host:port}) the certificate was presented for
     * @param cert the server's leaf certificate
     * @param sha256Fingerprint the certificate's SHA-256 fingerprint, colon-separated uppercase hex
     * @return the operator's decision
     */
    Decision confirm(String hostPort, X509Certificate cert, String sha256Fingerprint);
}
