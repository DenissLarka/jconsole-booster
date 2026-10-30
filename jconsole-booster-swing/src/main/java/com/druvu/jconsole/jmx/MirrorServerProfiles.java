package com.druvu.jconsole.jmx;

import com.druvu.jmxmp.shared.SelectProfiles;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Client-side JMXMP profile selector: on each connection JCB mirrors whatever transport the server advertises in its
 * {@code HandshakeBeginMessage}, intersected with what this build speaks ({@code TLS} + {@code SASL/PLAIN}).
 *
 * <ul>
 *   <li>Server offers {@code TLS SASL/PLAIN} (a druvu-lib-jmxmp 2.0.0 secure endpoint) &rarr; request exactly that, in
 *       TLS-then-SASL order (encrypt, then authenticate).
 *   <li>Server offers nothing (a legacy plaintext endpoint) &rarr; request nothing, i.e. connect in the clear.
 * </ul>
 *
 * <p>This is what lets a single JCB connection reach both legacy and hardened servers with no user-facing "use TLS"
 * toggle and no retry: the jmxmp handshake advertises the server's profiles before the client commits, and this
 * selector adapts in-band (see {@code AdminClient.selectProfiles}). It is installed under the
 * {@code com.sun.jmx.remote.profile.selector} env key by {@link JMXConnectionManager}. It requires the client policy to
 * be {@link com.druvu.jmxmp.shared.ClientProfilePolicy#unrestricted()} — the default mandatory-TLS policy would reject
 * a plaintext server before this selector ever runs.
 *
 * <p><b>Hard security rule:</b> if this connection carries credentials and the server offers no encryption, the
 * selector throws {@link PlaintextCredentialsRefusedException} — refusing the connection <em>before</em> the SASL
 * profile would transmit the password in the clear. jmxmp runs the selector during the handshake, before any
 * credential-bearing profile and on every reconnect, so this is the correct place to stop a TLS-strip / downgrade
 * attack: a stolen credential is a worse outcome than a refused session.
 */
final class MirrorServerProfiles implements SelectProfiles {

    private static final String TLS = "TLS";
    private static final String SASL_PLAIN = "SASL/PLAIN";

    private final boolean credentialsPresent;

    /** @param credentialsPresent whether this connection will authenticate (a username/password was supplied). */
    MirrorServerProfiles(boolean credentialsPresent) {
        this.credentialsPresent = credentialsPresent;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"}) // SelectProfiles is a raw-typed OpenDMK SPI; env is an in/out Map
    public void selectProfiles(Map env, String serverProfiles) throws PlaintextCredentialsRefusedException {
        Set<String> offered = tokenizeUpper(serverProfiles);
        boolean tls = offered.contains(TLS);
        // Never send credentials over an unencrypted transport. This runs before the SASL profile, so a refusal here
        // means the password is never put on the wire.
        if (credentialsPresent && !tls) {
            throw new PlaintextCredentialsRefusedException(
                    "Refusing to send credentials to a server that offered no encryption (plaintext)");
        }
        StringBuilder chosen = new StringBuilder();
        if (tls) {
            chosen.append(TLS);
        }
        if (offered.contains(SASL_PLAIN)) {
            if (chosen.length() > 0) {
                chosen.append(' ');
            }
            chosen.append(SASL_PLAIN);
        }
        if (chosen.length() == 0) {
            env.remove("jmx.remote.profiles");
        } else {
            env.put("jmx.remote.profiles", chosen.toString());
        }
    }

    private static Set<String> tokenizeUpper(String profiles) {
        Set<String> set = new HashSet<>();
        if (profiles != null) {
            for (String token : profiles.trim().split("\\s+")) {
                if (!token.isEmpty()) {
                    set.add(token.toUpperCase(Locale.ROOT));
                }
            }
        }
        return set;
    }
}
