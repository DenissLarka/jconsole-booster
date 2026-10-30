package com.druvu.jconsole.core.connect;

import java.io.IOException;
import java.util.Map;

/**
 * One JMX protocol's part of a connection: the entries it adds to the connector environment for the service URLs it
 * handles, {@code service:jmx:<protocol>://…}. Transports are found at run time through druvu-lib-loader; a protocol
 * that no transport handles, such as the JDK's own RMI, connects with the credentials alone.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public interface Transport {

    /** The protocol of the service URLs this transport handles: {@code jmxmp} for {@code service:jmx:jmxmp://…}. */
    String protocol();

    /** Adds this protocol's entries for one connection to {@code environment}. */
    void configure(Map<String, Object> environment, ConnectRequest request) throws IOException;
}
