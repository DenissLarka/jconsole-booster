package com.druvu.jconsole.core.connect;

import com.druvu.lib.loader.MultiComponentLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * The transports present at run time, by protocol: found once through druvu-lib-loader. Two transports for one protocol
 * are a packaging error, reported at the first connection rather than settled by load order.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class Transports {

    private static final Map<String, Transport> BY_PROTOCOL = load();

    private Transports() {}

    /** The transport for {@code protocol}, or {@code null} when none handles it. */
    static Transport forProtocol(String protocol) {
        return BY_PROTOCOL.get(protocol);
    }

    private static Map<String, Transport> load() {
        Map<String, Transport> byProtocol = new HashMap<>();
        for (Transport transport : MultiComponentLoader.loadAll(Transport.class)) {
            Transport previous = byProtocol.putIfAbsent(transport.protocol(), transport);
            if (previous != null) {
                throw new IllegalStateException("Two transports for the protocol " + transport.protocol() + ": "
                        + previous.getClass().getName() + " and "
                        + transport.getClass().getName());
            }
        }
        return Map.copyOf(byProtocol);
    }
}
