package com.druvu.jconsole.ui.dialogs;

import com.druvu.jconsole.jmx.PlaintextCredentialAlert;
import javax.swing.JOptionPane;

/**
 * Explains, prominently, that a connection was hard-refused because it would have sent the username/password over an
 * unencrypted (plaintext) transport. There is no "proceed anyway" — sending JMX credentials in the clear has no
 * legitimate use, and the refusal already happened before any password left the machine. {@link #asAlert()} returns the
 * {@link PlaintextCredentialAlert} installed at launch; it is safe to call from any thread (it marshals to the EDT).
 */
public final class PlaintextCredentialsDialog {

    private PlaintextCredentialsDialog() {}

    public static PlaintextCredentialAlert asAlert() {
        return PlaintextCredentialsDialog::show;
    }

    private static void show(String hostPort) {
        DialogSupport.onEdtAsync(() -> JOptionPane.showMessageDialog(
                DialogSupport.activeWindow(),
                "<html><span style='color:#b00020'><b>⚠ Connection refused — your password would have been sent"
                        + " UNENCRYPTED.</b></span><br><br>"
                        + "<b>" + DialogSupport.escapeHtml(hostPort)
                        + "</b> asked to authenticate over a <b>plaintext</b>"
                        + " (unencrypted) connection. Anyone on the network could read your username and password.<br><br>"
                        + "A server that requests a password without encryption is almost always either misconfigured"
                        + " or an attacker <b>stripping TLS</b> to steal credentials. JCB refused the connection"
                        + " <b>before</b> sending anything.<br><br>"
                        + "If you trust this endpoint, fix it to require TLS, then reconnect.</html>",
                "Unencrypted credentials refused",
                JOptionPane.ERROR_MESSAGE));
    }
}
