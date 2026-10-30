package com.druvu.jconsole.app.connect;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;

/**
 * The Connect dialog: asks which JVM to connect to and how. It only asks; connecting is the caller's job, and a failed
 * attempt comes back here with the reason in the status line.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class ConnectDialog {

    private final Window owner;
    private final Supplier<List<String>> recentConnections;
    private final Supplier<Map<String, String>> bookmarkLabels;
    private final BiConsumer<Window, String> addBookmark;

    /**
     * @param owner the window the dialog belongs to
     * @param recentConnections the recent connections, most recent first, read each time the dialog opens
     * @param bookmarkLabels the bookmarks' labels by target, read each time the dialog opens
     * @param addBookmark adds a bookmark for a target, given the window to put its own dialog over
     */
    public ConnectDialog(
            Window owner,
            Supplier<List<String>> recentConnections,
            Supplier<Map<String, String>> bookmarkLabels,
            BiConsumer<Window, String> addBookmark) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.recentConnections = Objects.requireNonNull(recentConnections, "recentConnections");
        this.bookmarkLabels = Objects.requireNonNull(bookmarkLabels, "bookmarkLabels");
        this.addBookmark = Objects.requireNonNull(addBookmark, "addBookmark");
    }

    /**
     * Shows the dialog and waits for the user.
     *
     * @param target the remote process to start with, empty for none
     * @param userName the user name to start with, or {@code null}
     * @param message why the last attempt failed, or {@code null}
     * @return what to connect to, or empty when the user cancelled
     */
    public Optional<ConnectionRequest> ask(String target, String userName, String message) {
        ConnectForm form = new ConnectForm(recentConnections.get(), bookmarkLabels.get());
        form.prefill(target, userName, message);

        Dialog<ConnectionRequest> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("New Connection");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(form.node());

        ButtonType bookmark = new ButtonType("Add to bookmarks", ButtonData.LEFT);
        ButtonType connect = new ButtonType("Connect", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(bookmark, connect, ButtonType.CANCEL);
        Node bookmarkButton = dialog.getDialogPane().lookupButton(bookmark);
        bookmarkButton.disableProperty().bind(form.valid().not());
        // Adding a bookmark keeps the dialog open: the user still wants to connect.
        bookmarkButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            addBookmark.accept(
                    dialog.getDialogPane().getScene().getWindow(),
                    form.request().shortUrl());
        });
        dialog.getDialogPane()
                .lookupButton(connect)
                .disableProperty()
                .bind(form.valid().not());
        dialog.setResultConverter(button -> button == connect ? form.request() : null);
        dialog.setOnShown(event -> Platform.runLater(form::focusRemote));
        return dialog.showAndWait();
    }
}
