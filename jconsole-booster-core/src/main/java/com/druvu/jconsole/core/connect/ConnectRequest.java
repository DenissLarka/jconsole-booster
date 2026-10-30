package com.druvu.jconsole.core.connect;

import com.druvu.jconsole.core.trust.CertTrustPrompt;
import java.util.Objects;
import javax.management.remote.JMXServiceURL;

/**
 * What a {@link Transport} needs to know about one connection: the target, whether credentials travel with it, and how
 * to treat a server certificate that does not chain to a trusted CA.
 *
 * @param url the target's service URL
 * @param withCredentials whether the connection authenticates with a user name and password
 * @param trustPrompt asked to approve an untrusted certificate (trust on first use)
 * @param requireValidChain when {@code true}, an untrusted certificate is rejected without asking
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record ConnectRequest(
        JMXServiceURL url, boolean withCredentials, CertTrustPrompt trustPrompt, boolean requireValidChain) {

    public ConnectRequest {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(trustPrompt, "trustPrompt");
    }

    /** {@code host:port}, the key under which a certificate's trust is remembered. */
    public String hostPort() {
        return url.getHost() + ":" + url.getPort();
    }
}
