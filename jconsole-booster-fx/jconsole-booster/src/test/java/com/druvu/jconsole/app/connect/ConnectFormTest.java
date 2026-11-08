package com.druvu.jconsole.app.connect;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.FxTestToolkit;
import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Covers the Connect dialog's form: Connect follows the target, the request it builds, prefill. */
public class ConnectFormTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void connectWaitsForAConnectableTarget() {
        List<Boolean> validity = FxTestToolkit.call(() -> {
            ConnectForm form = new ConnectForm();
            boolean empty = form.valid().get();
            form.remoteField().setText("localhost");
            boolean hostOnly = form.valid().get();
            form.remoteField().setText("localhost:7091");
            boolean hostAndPort = form.valid().get();
            return List.of(empty, hostOnly, hostAndPort);
        });

        assertThat(validity).containsExactly(false, false, true);
    }

    @Test
    public void theRequestCarriesWhatWasTyped() {
        ConnectionRequest request = FxTestToolkit.call(() -> {
            ConnectForm form = new ConnectForm();
            form.remoteField().setText(" localhost:7091 ");
            form.userNameField().setText(" admin ");
            form.passwordField().setText("admin");
            form.requireValidChainField().setSelected(true);
            return form.request();
        });

        assertThat(request)
                .isEqualTo(new ConnectionRequest("service:jmx:jmxmp://localhost:7091", "admin", "admin", true));
    }

    @Test
    public void emptyCredentialsAreNone() {
        ConnectionRequest request = FxTestToolkit.call(() -> {
            ConnectForm form = new ConnectForm();
            form.remoteField().setText("localhost:7091");
            return form.request();
        });

        assertThat(request.userName()).isNull();
        assertThat(request.password()).isNull();
    }

    @Test
    public void aFailedAttemptComesBackWithItsReasonInRed() {
        List<Object> shown = FxTestToolkit.call(() -> {
            ConnectForm form = new ConnectForm();
            form.passwordField().setText("old");
            form.prefill("h:1", "admin", "Connection failed: Connection refused");
            return List.of(
                    form.remoteField().getText(),
                    form.userNameField().getText(),
                    form.passwordField().getText(),
                    form.statusText(),
                    form.statusIsError());
        });

        assertThat(shown).containsExactly("h:1", "admin", "", "Connection failed: Connection refused", true);
    }

    @Test
    public void withoutAReasonTheStatusLineSaysWhatToDo() {
        List<Object> shown = FxTestToolkit.call(() -> {
            ConnectForm form = new ConnectForm();
            form.prefill("h:1", null, null);
            return List.of(form.statusText(), form.statusIsError());
        });

        assertThat(shown).containsExactly(ConnectForm.READY, false);
    }
}
