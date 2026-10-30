package com.druvu.jconsole.app.connect;

import com.druvu.jconsole.core.trust.CertTrustPrompt;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import javafx.stage.Window;

/**
 * Asks the user about an untrusted server certificate in a dialog. The connect runs on a worker thread, which waits
 * here until the user answers; anything that keeps the question from being asked answers Cancel.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class FxCertTrustPrompt implements CertTrustPrompt {

    private final Window owner;

    /** @param owner the window the dialogs belong to */
    public FxCertTrustPrompt(Window owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public Decision confirm(String hostPort, X509Certificate certificate, String fingerprint) {
        return OnFxThread.ask(
                () -> CertTrustDialogs.firstUse(owner, hostPort, certificate, fingerprint)
                        .showAndWait()
                        .orElse(Decision.CANCEL),
                Decision.CANCEL);
    }

    @Override
    public Decision confirmChanged(
            String hostPort, X509Certificate certificate, String fingerprint, List<String> previousFingerprints) {
        return OnFxThread.ask(
                () -> CertTrustDialogs.changed(owner, hostPort, certificate, fingerprint, previousFingerprints)
                        .showAndWait()
                        .orElse(Decision.CANCEL),
                Decision.CANCEL);
    }
}
