package com.druvu.jconsole.console;

import com.druvu.jconsole.jmx.PlaintextCredentialAlert;

/**
 * Console implementation of {@link PlaintextCredentialAlert}: explains why a connection was hard-refused for trying to
 * send credentials over an unencrypted transport. The refusal has already happened and is non-overridable — this is
 * purely informational, the console analog of the GUI's {@code PlaintextCredentialsDialog}.
 */
final class ConsolePlaintextAlert implements PlaintextCredentialAlert {

    private final ConsoleIO io;

    ConsolePlaintextAlert(ConsoleIO io) {
        this.io = io;
    }

    @Override
    public void refused(String hostPort) {
        io.println();
        io.println("REFUSED: " + hostPort + " offered no encryption while credentials were supplied.");
        io.println("Credentials are never sent over an unencrypted transport — this policy cannot be overridden.");
        io.println("The endpoint may be a legacy plaintext server or an impostor. Connect without a user to reach");
        io.println("an anonymous plaintext server, or point at a TLS-enabled endpoint.");
    }
}
