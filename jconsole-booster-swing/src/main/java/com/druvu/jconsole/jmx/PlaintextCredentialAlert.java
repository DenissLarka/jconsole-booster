package com.druvu.jconsole.jmx;

/**
 * Notified when a connection was hard-refused because it would have sent credentials over an unencrypted transport (see
 * {@link PlaintextCredentialsRefusedException}). This is purely informational — the refusal has already happened and
 * cannot be overridden; the alert exists so the UI can explain, prominently, why the connection was blocked.
 * {@code JConsole} installs an interactive dialog at launch; the default is a no-op for headless callers.
 */
public interface PlaintextCredentialAlert {

    /** @param hostPort the endpoint ({@code host:port}) whose plaintext-credential connection was refused. */
    void refused(String hostPort);
}
