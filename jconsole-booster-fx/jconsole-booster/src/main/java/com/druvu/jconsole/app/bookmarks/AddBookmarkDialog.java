package com.druvu.jconsole.app.bookmarks;

import atlantafx.base.theme.Styles;
import com.druvu.jconsole.core.bookmarks.BookmarkWriter;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * Asks for a new bookmark's label and group. The group is picked from the file's groups or typed as a new one; OK waits
 * until both are filled in, because a bookmark without a label defeats its point. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class AddBookmarkDialog {

    private final TextField label = new TextField();
    private final ComboBox<String> group = new ComboBox<>();
    private final Dialog<NewBookmark> dialog = new Dialog<>();
    private final BooleanBinding complete;

    /**
     * @param owner the window the dialog belongs to
     * @param target the bookmark's target, shown for reference
     * @param groupNames the groups already in the file
     */
    AddBookmarkDialog(Window owner, String target, List<String> groupNames) {
        group.setEditable(true);
        group.getItems().setAll(groupNames);
        group.getEditor().setText(BookmarkWriter.DEFAULT_GROUP);
        label.setPromptText("e.g. *prod-1*  or  [red prod-1]");
        complete = Bindings.createBooleanBinding(
                () -> !label.getText().isBlank() && !group.getEditor().getText().isBlank(),
                label.textProperty(),
                group.getEditor().textProperty());

        Label targetLabel = new Label(target);
        targetLabel.getStyleClass().add(Styles.TEXT_BOLD);
        GridPane form = new GridPane(10, 8);
        form.addRow(0, new Label("Bookmark for"), targetLabel);
        form.addRow(1, new Label("Label"), label);
        form.addRow(2, new Label("Group"), group);

        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Add bookmark");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(complete.not());
        // The group is read from the editor: a newly typed group is not committed to the combo's value yet.
        dialog.setResultConverter(button -> button == ButtonType.OK
                ? new NewBookmark(
                        group.getEditor().getText().strip(), label.getText().strip())
                : null);
    }

    Dialog<NewBookmark> dialog() {
        return dialog;
    }

    /** Whether label and group are both filled in; OK follows it. */
    boolean isComplete() {
        return complete.get();
    }

    TextField labelField() {
        return label;
    }

    ComboBox<String> groupField() {
        return group;
    }
}
