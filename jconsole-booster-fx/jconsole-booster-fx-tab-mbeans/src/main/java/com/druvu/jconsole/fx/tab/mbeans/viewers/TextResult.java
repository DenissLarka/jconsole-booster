package com.druvu.jconsole.fx.tab.mbeans.viewers;

import com.druvu.jconsole.core.text.Messages;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

/**
 * A value as monospace text the user can select and copy; long strings and dumps are painful to grab by mouse.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class TextResult {

    private final TextArea text = new TextArea();
    private final BorderPane pane = new BorderPane(text);

    public TextResult(String value) {
        text.setText(value);
        text.setEditable(false);
        text.setFont(Font.font("Monospaced", 12));
        Button selectAll = new Button(Messages.OPERATION_RETURN_VALUE_SELECT_ALL);
        selectAll.setOnAction(event -> {
            text.requestFocus();
            text.selectAll();
        });
        Button copy = new Button(Messages.OPERATION_RETURN_VALUE_COPY);
        copy.setOnAction(event -> copy());
        HBox bar = new HBox(6, selectAll, copy);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(0, 0, 6, 0));
        pane.setTop(bar);
    }

    public Node node() {
        return pane;
    }

    /** Copies the selection; with nothing selected, everything, so the button never does nothing. */
    private void copy() {
        String selected = text.getSelectedText();
        ClipboardContent content = new ClipboardContent();
        content.putString(selected.isEmpty() ? text.getText() : selected);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
