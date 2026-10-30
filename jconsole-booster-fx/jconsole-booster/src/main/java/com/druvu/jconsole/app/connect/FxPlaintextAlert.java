package com.druvu.jconsole.app.connect;

import com.druvu.jconsole.core.connect.PlaintextCredentialAlert;
import com.druvu.lib.fx.util.FxThreads;
import java.util.Objects;
import javafx.scene.control.Alert;
import javafx.stage.Window;

/**
 * Explains a connection refused because it would have sent the user name and password unencrypted. The refusal has
 * already happened and cannot be overridden, so the alert offers no way on; the worker does not wait for it.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class FxPlaintextAlert implements PlaintextCredentialAlert {

    private final Window owner;

    /** @param owner the window the alert belongs to */
    public FxPlaintextAlert(Window owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public void refused(String hostPort) {
        FxThreads.onFx(() -> show(hostPort));
    }

    private void show(String hostPort) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("Unencrypted credentials refused");
        alert.setHeaderText("Connection refused: your password would have been sent UNENCRYPTED.");
        alert.setContentText(hostPort + " asked to authenticate over a plaintext connection, where anyone on the"
                + " network could read the user name and password.\n\n"
                + "A server that asks for a password without encryption is either misconfigured or someone stripping"
                + " TLS to steal credentials. JConsole Booster refused the connection before sending anything.\n\n"
                + "If you trust this endpoint, make it require TLS, then connect again.");
        alert.show();
    }
}
