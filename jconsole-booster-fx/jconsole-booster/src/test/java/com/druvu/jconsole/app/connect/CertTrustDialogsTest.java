package com.druvu.jconsole.app.connect;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.FxTestToolkit;
import com.druvu.jconsole.core.trust.CertTrustPrompt.Decision;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Covers the certificate questions: what they show, that Enter cancels, and how the buttons map to a decision. */
public class CertTrustDialogsTest {

    private static final String FINGERPRINT = "AB:CD:EF:01";

    // A self-signed certificate for CN=sample-target, O=druvu, made with keytool.
    private static final String CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIIBaTCCAQ+gAwIBAgIIFbmC5Fni6a0wCgYIKoZIzj0EAwMwKDEOMAwGA1UEChMF
            ZHJ1dnUxFjAUBgNVBAMTDXNhbXBsZS10YXJnZXQwIBcNMjYxMDMwMjIyMDAyWhgP
            MjEyNjEwMDYyMjIwMDJaMCgxDjAMBgNVBAoTBWRydXZ1MRYwFAYDVQQDEw1zYW1w
            bGUtdGFyZ2V0MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEkArqlhNQ238J36cX
            gCLa7G4hVqBJZ38xpaCQyTAWHLTdGPifsI2vwweAvBzzJo1+Gp7fBYvNBp6hghMF
            S/iK0KMhMB8wHQYDVR0OBBYEFMBNvuLfey7GyHKk6QaVAcNHQqV0MAoGCCqGSM49
            BAMDA0gAMEUCIGl3qf5a4siJbTAZ1HfAzZ5Z+2JqLbkQNYFE5YmJP80MAiEAxQOK
            K4m8F5dhWGGv7bs9YAKDDRIvZQ0301dN8qOcNbw=
            -----END CERTIFICATE-----
            """;

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void aFirstUseShowsTheFingerprintAndTheCertificatesIdentity() throws CertificateException {
        X509Certificate certificate = certificate();

        String details = FxTestToolkit.call(() -> detailsOf(firstUse(certificate)));

        assertThat(details)
                .contains(FINGERPRINT)
                .contains("Subject: CN=sample-target,O=druvu")
                .contains("Issuer:  CN=sample-target,O=druvu");
    }

    @Test
    public void aChangedCertificateAlsoShowsThePreviouslyTrustedOnes() throws CertificateException {
        X509Certificate certificate = certificate();

        String details = FxTestToolkit.call(() ->
                detailsOf(CertTrustDialogs.changed(null, "h:1", certificate, FINGERPRINT, List.of("0102AB", "FFEE"))));

        assertThat(details)
                .contains("New SHA-256 fingerprint:")
                .contains("01:02:AB")
                .contains("FF:EE");
    }

    @Test
    public void enterCancels() throws CertificateException {
        X509Certificate certificate = certificate();

        boolean cancelIsDefault = FxTestToolkit.call(() -> {
            Dialog<Decision> dialog = firstUse(certificate);
            Button cancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            Button trust = (Button) dialog.getDialogPane().lookupButton(trustButton(dialog));
            return cancel.isDefaultButton() && !trust.isDefaultButton();
        });

        assertThat(cancelIsDefault).isTrue();
    }

    @Test
    public void theButtonsAndTheCheckboxMakeTheDecision() throws CertificateException {
        X509Certificate certificate = certificate();

        List<Decision> decisions = FxTestToolkit.call(() -> {
            Dialog<Decision> dialog = firstUse(certificate);
            ButtonType trust = trustButton(dialog);
            CheckBox remember = (CheckBox) dialog.getDialogPane().lookup("#" + CertTrustDialogs.REMEMBER_ID);
            Decision once = dialog.getResultConverter().call(trust);
            Decision cancelled = dialog.getResultConverter().call(ButtonType.CANCEL);
            remember.setSelected(true);
            Decision remembered = dialog.getResultConverter().call(trust);
            Decision closed = dialog.getResultConverter().call(null);
            return List.of(once, cancelled, remembered, closed);
        });

        assertThat(decisions)
                .containsExactly(Decision.TRUST_ONCE, Decision.CANCEL, Decision.TRUST_REMEMBER, Decision.CANCEL);
    }

    private static Dialog<Decision> firstUse(X509Certificate certificate) {
        return CertTrustDialogs.firstUse(null, "localhost:7091", certificate, FINGERPRINT);
    }

    private static ButtonType trustButton(Dialog<Decision> dialog) {
        return dialog.getDialogPane().getButtonTypes().stream()
                .filter(button -> button.getButtonData() == ButtonData.OK_DONE)
                .findFirst()
                .orElseThrow();
    }

    private static String detailsOf(Dialog<Decision> dialog) {
        return dialog.getDialogPane().getContent().lookupAll(".text-area").stream()
                .map(node -> ((TextArea) node).getText())
                .findFirst()
                .orElseThrow();
    }

    private static X509Certificate certificate() throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(CERTIFICATE.getBytes(StandardCharsets.US_ASCII)));
    }
}
