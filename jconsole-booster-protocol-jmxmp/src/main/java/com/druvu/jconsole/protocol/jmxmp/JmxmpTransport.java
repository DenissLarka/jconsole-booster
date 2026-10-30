package com.druvu.jconsole.protocol.jmxmp;

import com.druvu.jconsole.core.connect.ConnectRequest;
import com.druvu.jconsole.core.connect.Transport;
import com.druvu.jconsole.core.trust.TofuTrustManager;
import com.druvu.jmxmp.shared.ClientProfilePolicy;
import java.io.IOException;
import java.util.Map;

/**
 * JMXMP's part of a connection. One connection reaches legacy plaintext servers and hardened druvu-lib-jmxmp servers
 * (TLS and SASL/PLAIN) alike: the JMXMP handshake advertises the server's profiles before the client commits, and
 * {@link MirrorServerProfiles} mirrors them, so there is no "use TLS" toggle and no retry. On the TLS leg the server's
 * certificate goes through trust on first use. druvu-lib-jmxmp's serial filter keeps deserialization default-deny on
 * either leg.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class JmxmpTransport implements Transport {

    @Override
    public String protocol() {
        return "jmxmp";
    }

    @Override
    public void configure(Map<String, Object> environment, ConnectRequest request) throws IOException {
        // Unrestricted, so the selector is free to pick either leg: the default mandatory-TLS client policy would
        // reject a plaintext server before the selector runs.
        environment.put(ClientProfilePolicy.ENV_KEY, ClientProfilePolicy.unrestricted());
        environment.put("com.sun.jmx.remote.profile.selector", new MirrorServerProfiles(request.withCredentials()));
        environment.put(
                "jmx.remote.tls.socket.factory",
                TofuTrustManager.socketFactory(request.hostPort(), request.trustPrompt(), request.requireValidChain()));
    }
}
