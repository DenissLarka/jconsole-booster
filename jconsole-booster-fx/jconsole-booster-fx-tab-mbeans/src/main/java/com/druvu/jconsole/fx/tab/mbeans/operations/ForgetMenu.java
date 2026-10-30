package com.druvu.jconsole.fx.tab.mbeans.operations;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;

/**
 * The context menu of a parameter input: "Forget this value", after the usual edit actions for a text input, whose own
 * menu it replaces.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ForgetMenu {

    static final String FORGET = "Forget this value";

    private ForgetMenu() {}

    static void install(Control control, Runnable forget) {
        MenuItem forgetItem = new MenuItem(FORGET);
        forgetItem.setOnAction(event -> forget.run());
        if (control instanceof TextInputControl text) {
            text.setContextMenu(new ContextMenu(
                    item("Cut", text::cut),
                    item("Copy", text::copy),
                    item("Paste", text::paste),
                    new SeparatorMenuItem(),
                    item("Select all", text::selectAll),
                    new SeparatorMenuItem(),
                    forgetItem));
        } else {
            control.setContextMenu(new ContextMenu(forgetItem));
        }
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
    }
}
