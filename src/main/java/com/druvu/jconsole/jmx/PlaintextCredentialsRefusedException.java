package com.druvu.jconsole.jmx;

import java.io.IOException;

/**
 * Thrown from the profile selector during the handshake when a connection carrying credentials would come up
 * unencrypted (the server offered no TLS). It aborts the connection <em>before</em> the SASL profile transmits the
 * password, so the credential never reaches the wire. Almost always this means a misconfiguration or a TLS-strip
 * attack; there is no legitimate case for sending a JMX password in the clear, so JCB hard-refuses.
 */
public final class PlaintextCredentialsRefusedException extends IOException {

    public PlaintextCredentialsRefusedException(String message) {
        super(message);
    }
}
