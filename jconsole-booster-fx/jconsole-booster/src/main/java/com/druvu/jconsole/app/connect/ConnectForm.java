package com.druvu.jconsole.app.connect;

import atlantafx.base.theme.Styles;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The fields of the Connect dialog and the request they add up to: the remote process, the credentials, and the
 * certificate policy, with a status line under them. The form remembers nothing: a JVM used often is a bookmark. FX
 * thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ConnectForm {

    static final String READY = "Enter a connection (host:port) and press Connect.";
    static final String ERROR_STYLE = Styles.DANGER;

    private static final String USAGE = "host:port or service:jmx:<protocol>:<address>";
    private static final double FIELD_WIDTH = 420;

    private final TextField remote = new TextField();
    private final TextField userName = new TextField();
    private final PasswordField password = new PasswordField();
    private final CheckBox requireValidChain =
            new CheckBox("Require a CA-validated certificate (no trust-on-first-use)");
    private final Label status = new Label(READY);
    private final BooleanBinding valid;
    private final VBox node;

    ConnectForm() {
        remote.setMaxWidth(Double.MAX_VALUE);
        remote.setPrefWidth(FIELD_WIDTH);
        valid = Bindings.createBooleanBinding(() -> RemoteAddress.isValid(remote.getText()), remote.textProperty());

        requireValidChain.setTooltip(
                new Tooltip(
                        "Connect only if the server's TLS certificate chains to a trusted CA."
                                + " A self-signed certificate is refused instead of offered for trust: for servers behind a real PKI."));
        status.setWrapText(true);
        status.setMaxWidth(FIELD_WIDTH);

        Label usage = new Label(USAGE);
        usage.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        GridPane fields = new GridPane(10, 8);
        fields.addRow(0, bold("Remote process"), remote);
        fields.add(usage, 1, 1);
        fields.addRow(2, bold("Username"), userName);
        fields.addRow(3, bold("Password"), password);
        fields.add(requireValidChain, 1, 4);
        GridPane.setHgrow(remote, Priority.ALWAYS);
        node = new VBox(16, fields, status);
    }

    Node node() {
        return node;
    }

    /** Whether the remote process can be connected to; the Connect button follows it. */
    BooleanBinding valid() {
        return valid;
    }

    /** Fills the form, e.g. with a bookmark's target or with a failed attempt and why it failed. */
    void prefill(String target, String user, String message) {
        remote.setText(target == null ? "" : target);
        userName.setText(user == null ? "" : user);
        password.clear();
        showStatus(message);
    }

    /** The request the form adds up to; call only while {@link #valid()} holds. */
    ConnectionRequest request() {
        String user = userName.getText().strip();
        String secret = password.getText();
        ConnectionRequest target = ConnectionRequest.to(remote.getText());
        return new ConnectionRequest(
                target.url(),
                user.isEmpty() ? null : user,
                secret.isEmpty() ? null : secret,
                requireValidChain.isSelected());
    }

    /** Puts the cursor in the remote process, with its text selected so typing replaces it. */
    void focusRemote() {
        remote.requestFocus();
        remote.selectAll();
    }

    /** The status line's text and whether it reads as an error, for tests. */
    String statusText() {
        return status.getText();
    }

    boolean statusIsError() {
        return status.getStyleClass().contains(ERROR_STYLE);
    }

    TextField remoteField() {
        return remote;
    }

    TextField userNameField() {
        return userName;
    }

    PasswordField passwordField() {
        return password;
    }

    CheckBox requireValidChainField() {
        return requireValidChain;
    }

    private void showStatus(String message) {
        boolean error = message != null && !message.isBlank();
        status.setText(error ? message : READY);
        status.getStyleClass().remove(ERROR_STYLE);
        if (error) {
            status.getStyleClass().add(ERROR_STYLE);
        }
    }

    private static Label bold(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(Styles.TEXT_BOLD);
        return label;
    }
}
