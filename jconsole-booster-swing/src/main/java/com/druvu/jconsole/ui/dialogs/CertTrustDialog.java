package com.druvu.jconsole.ui.dialogs;

import com.druvu.jconsole.jmx.CertTrustPrompt;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Trust-on-first-use dialog: shows a server certificate's fingerprint and identity and asks the operator whether to
 * trust it, with an optional "remember" checkbox. Two variants — a first-use prompt and a louder "certificate changed"
 * prompt. {@link #asPrompt()} returns the {@link CertTrustPrompt} installed at launch; it is safe to call from any
 * thread (it marshals to the Swing EDT).
 */
public final class CertTrustDialog {

    private CertTrustDialog() {}

    /**
     * The interactive {@link CertTrustPrompt} — installed at launch via
     * {@code JMXConnectionManager.setCertTrustPrompt}. Returned as an instance (not a method reference) so both the
     * first-use and the certificate-changed paths are carried.
     */
    public static CertTrustPrompt asPrompt() {
        return new CertTrustPrompt() {
            @Override
            public Decision confirm(String hostPort, X509Certificate cert, String sha256Fingerprint) {
                return DialogSupport.onEdt(
                        () -> show(hostPort, cert, sha256Fingerprint), CertTrustPrompt.Decision.CANCEL);
            }

            @Override
            public Decision confirmChanged(
                    String hostPort, X509Certificate cert, String sha256Fingerprint, List<String> previous) {
                return DialogSupport.onEdt(
                        () -> showChanged(hostPort, cert, sha256Fingerprint, previous),
                        CertTrustPrompt.Decision.CANCEL);
            }
        };
    }

    private static CertTrustPrompt.Decision show(String hostPort, X509Certificate cert, String fingerprint) {
        JLabel header = new JLabel(
                "<html>The server <b>" + DialogSupport.escapeHtml(hostPort) + "</b> presented a certificate that is"
                        + " <b>not signed by a trusted authority</b>.<br>Verify the fingerprint out-of-band before trusting"
                        + " it.</html>");

        JTextArea details = new JTextArea("SHA-256 fingerprint:\n    " + fingerprint + "\n\nSubject: "
                + cert.getSubjectX500Principal().getName() + "\nIssuer:  "
                + cert.getIssuerX500Principal().getName());
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        details.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JScrollPane scroll = new JScrollPane(details);
        scroll.setPreferredSize(new Dimension(560, 120));

        JCheckBox remember = new JCheckBox("Remember this certificate for future connections to this host");

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(remember, BorderLayout.SOUTH);

        Object[] options = {"Trust", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                DialogSupport.activeWindow(),
                panel,
                "Untrusted server certificate",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);

        if (choice != 0) {
            return CertTrustPrompt.Decision.CANCEL;
        }
        return remember.isSelected() ? CertTrustPrompt.Decision.TRUST_REMEMBER : CertTrustPrompt.Decision.TRUST_ONCE;
    }

    private static CertTrustPrompt.Decision showChanged(
            String hostPort, X509Certificate cert, String fingerprint, List<String> previous) {
        JLabel header = new JLabel(
                "<html><span style='color:#b00020'><b>⚠ The certificate for "
                        + DialogSupport.escapeHtml(hostPort) + " has CHANGED.</b></span><br>"
                        + "It no longer matches the certificate you previously trusted. This is expected if the server was"
                        + " restarted (its self-signed certificate is regenerated), but it can also mean someone is"
                        + " <b>intercepting the connection</b>. Verify the new fingerprint out-of-band before trusting it.</html>");

        StringBuilder prev = new StringBuilder();
        for (String p : previous) {
            prev.append("\n    ").append(withColons(p));
        }
        JTextArea details = new JTextArea("New SHA-256 fingerprint:\n    " + fingerprint
                + "\n\nPreviously trusted:" + prev + "\n\nSubject: "
                + cert.getSubjectX500Principal().getName() + "\nIssuer:  "
                + cert.getIssuerX500Principal().getName());
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        details.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JScrollPane scroll = new JScrollPane(details);
        scroll.setPreferredSize(new Dimension(560, 160));

        JCheckBox remember = new JCheckBox("Replace the saved certificate for this host with the new one");

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(remember, BorderLayout.SOUTH);

        Object[] options = {"Trust new certificate", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                DialogSupport.activeWindow(),
                panel,
                "Server certificate changed",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);

        if (choice != 0) {
            return CertTrustPrompt.Decision.CANCEL;
        }
        return remember.isSelected() ? CertTrustPrompt.Decision.TRUST_REMEMBER : CertTrustPrompt.Decision.TRUST_ONCE;
    }

    /** Formats a canonical (colon-less) hex fingerprint with colon separators for display. */
    private static String withColons(String canonHex) {
        if (canonHex == null || canonHex.length() < 2) {
            return canonHex == null ? "" : canonHex;
        }
        StringBuilder sb = new StringBuilder(canonHex.length() * 3 / 2);
        for (int i = 0; i < canonHex.length(); i += 2) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(canonHex, i, Math.min(i + 2, canonHex.length()));
        }
        return sb.toString();
    }
}
