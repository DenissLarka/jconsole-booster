package com.druvu.jconsole.app.connect;

import atlantafx.base.theme.Styles;
import com.druvu.jconsole.core.trust.CertTrustPrompt.Decision;
import java.security.cert.X509Certificate;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Window;

/**
 * The two certificate questions of trust on first use: a certificate never seen for the host, and one that differs from
 * the certificate trusted before. Enter answers Cancel in both, so a reflex never trusts.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class CertTrustDialogs {

    static final String REMEMBER_ID = "remember";

    // wide enough for a SHA-256 fingerprint on one monospaced line
    private static final double DETAILS_WIDTH = 760;

    private CertTrustDialogs() {}

    /** The question for a certificate this host has not presented before. */
    static Dialog<Decision> firstUse(Window owner, String hostPort, X509Certificate certificate, String fingerprint) {
        Label headline = wrapped(
                "The server " + hostPort + " presented a certificate that is not signed by a trusted authority.");
        Label advice = wrapped("Compare the fingerprint with the server's through another channel before trusting it.");
        String details = "SHA-256 fingerprint:\n    " + fingerprint + "\n\n" + identity(certificate);
        return question(
                owner,
                "Untrusted server certificate",
                new VBox(8, headline, advice),
                details,
                "Remember this certificate for future connections to this host",
                "Trust");
    }

    /** The louder question for a certificate that differs from the one trusted before for this host. */
    static Dialog<Decision> changed(
            Window owner,
            String hostPort,
            X509Certificate certificate,
            String fingerprint,
            List<String> previousFingerprints) {
        Label headline = wrapped("The certificate for " + hostPort + " has CHANGED.");
        headline.getStyleClass().addAll(Styles.DANGER, Styles.TEXT_BOLD);
        Label advice = wrapped("It no longer matches the certificate you trusted before. A restarted server regenerates"
                + " its self-signed certificate, which is harmless, but this is also what someone intercepting the"
                + " connection looks like. Compare the new fingerprint with the server's through another channel"
                + " before trusting it.");
        StringBuilder details = new StringBuilder("New SHA-256 fingerprint:\n    ")
                .append(fingerprint)
                .append("\n\nPreviously trusted:");
        for (String previous : previousFingerprints) {
            details.append("\n    ").append(Fingerprints.withColons(previous));
        }
        details.append("\n\n").append(identity(certificate));
        return question(
                owner,
                "Server certificate changed",
                new VBox(8, headline, advice),
                details.toString(),
                "Replace the saved certificate for this host with the new one",
                "Trust new certificate");
    }

    private static Dialog<Decision> question(
            Window owner, String title, VBox explanation, String details, String rememberText, String trustText) {
        TextArea detailsArea = new TextArea(details);
        detailsArea.setEditable(false);
        detailsArea.setWrapText(false);
        detailsArea.setFont(Font.font("Monospaced", 12));
        detailsArea.setPrefColumnCount(100);
        detailsArea.setPrefRowCount(details.lines().count() > 8 ? 10 : 7);
        CheckBox remember = new CheckBox(rememberText);
        remember.setId(REMEMBER_ID);

        Dialog<Decision> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        VBox content = new VBox(12, explanation, detailsArea, remember);
        content.setPrefWidth(DETAILS_WIDTH);
        dialog.getDialogPane().setContent(content);

        ButtonType trust = new ButtonType(trustText, ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(trust, ButtonType.CANCEL);
        // JavaFX makes the OK-type button the default one; Cancel takes that role here (it keeps Escape as well).
        ((Button) dialog.getDialogPane().lookupButton(trust)).setDefaultButton(false);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
        dialog.setResultConverter(button -> {
            if (button != trust) {
                return Decision.CANCEL;
            }
            return remember.isSelected() ? Decision.TRUST_REMEMBER : Decision.TRUST_ONCE;
        });
        return dialog;
    }

    private static String identity(X509Certificate certificate) {
        return "Subject: " + certificate.getSubjectX500Principal().getName() + "\nIssuer:  "
                + certificate.getIssuerX500Principal().getName();
    }

    private static Label wrapped(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(DETAILS_WIDTH);
        return label;
    }
}
