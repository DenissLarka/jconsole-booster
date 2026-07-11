package com.druvu.jconsole.ui.dialogs;

import com.druvu.jconsole.jmx.CertTrustPrompt;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Trust-on-first-use dialog: shows an untrusted server certificate's fingerprint and identity and asks the operator
 * whether to trust it, with an optional "remember" checkbox. {@link #prompt} is a {@link CertTrustPrompt} and is safe
 * to call from any thread (it marshals to the Swing EDT).
 */
public final class CertTrustDialog {

    private CertTrustDialog() {}

    /**
     * {@link CertTrustPrompt} entry point — installed at launch via {@code JMXConnectionManager.setCertTrustPrompt}.
     */
    public static CertTrustPrompt.Decision prompt(String hostPort, X509Certificate cert, String sha256Fingerprint) {
        if (SwingUtilities.isEventDispatchThread()) {
            return show(hostPort, cert, sha256Fingerprint);
        }
        AtomicReference<CertTrustPrompt.Decision> result = new AtomicReference<>(CertTrustPrompt.Decision.CANCEL);
        try {
            SwingUtilities.invokeAndWait(() -> result.set(show(hostPort, cert, sha256Fingerprint)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CertTrustPrompt.Decision.CANCEL;
        } catch (InvocationTargetException e) {
            return CertTrustPrompt.Decision.CANCEL;
        }
        return result.get();
    }

    private static CertTrustPrompt.Decision show(String hostPort, X509Certificate cert, String fingerprint) {
        JLabel header =
                new JLabel("<html>The server <b>" + escapeHtml(hostPort) + "</b> presented a certificate that is"
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
                activeWindow(),
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

    private static Window activeWindow() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
