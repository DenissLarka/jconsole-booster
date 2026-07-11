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
 */
final class MirrorServerProfiles implements SelectProfiles {

    private static final String TLS = "TLS";
    private static final String SASL_PLAIN = "SASL/PLAIN";

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"}) // SelectProfiles is a raw-typed OpenDMK SPI; env is an in/out Map
    public void selectProfiles(Map env, String serverProfiles) {
        Set<String> offered = tokenizeUpper(serverProfiles);
        StringBuilder chosen = new StringBuilder();
        if (offered.contains(TLS)) {
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
