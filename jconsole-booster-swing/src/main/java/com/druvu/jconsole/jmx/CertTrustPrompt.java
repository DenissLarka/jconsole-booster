package com.druvu.jconsole.jmx;

import java.security.cert.X509Certificate;
import java.util.List;

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

    /**
     * Asks the operator about a server certificate that <b>differs from one already pinned</b> for this endpoint — the
     * SSH {@code known_hosts} "REMOTE HOST IDENTIFICATION HAS CHANGED" case. This is either benign (the server rotated
     * its certificate — routine with druvu-lib-jmxmp's default ephemeral self-signed cert, which is regenerated on
     * every restart) or an active man-in-the-middle. Implementations should present this more prominently than a
     * first-use prompt and must not default to trust. A {@link Decision#TRUST_REMEMBER} here means <em>replace</em> the
     * old pin, not add another.
     *
     * <p>The default implementation delegates to {@link #confirm} so non-UI callers stay fail-closed.
     *
     * @param hostPort the target endpoint ({@code host:port})
     * @param cert the newly presented leaf certificate
     * @param sha256Fingerprint the new certificate's SHA-256 fingerprint, colon-separated uppercase hex
     * @param previousFingerprints the fingerprint(s) previously pinned for this endpoint (canonical hex)
     * @return the operator's decision
     */
    default Decision confirmChanged(
            String hostPort, X509Certificate cert, String sha256Fingerprint, List<String> previousFingerprints) {
        return confirm(hostPort, cert, sha256Fingerprint);
    }
}
