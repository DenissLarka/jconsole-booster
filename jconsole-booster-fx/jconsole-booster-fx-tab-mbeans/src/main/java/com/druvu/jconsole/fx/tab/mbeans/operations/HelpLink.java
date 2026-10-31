package com.druvu.jconsole.fx.tab.mbeans.operations;

import atlantafx.base.controls.Popover;
import javafx.scene.Cursor;
import javafx.scene.control.Label;

/**
 * The small hint above an input or a button, a parameter's name or an operation's return type, that opens its help in a
 * popover when clicked. It reads like a link: underlined under the mouse. The popover closes on a click elsewhere.
 *
 * @author Deniss Larka <br>
 *     on 31 Oct 2026
 */
final class HelpLink {

    /** Smaller and quieter than the value below it. */
    private static final String STYLE = "-fx-font-size: 0.85em; -fx-text-fill: -color-fg-muted;";

    /** The help's width before its text wraps. */
    private static final double HELP_WIDTH = 320;

    private HelpLink() {}

    /**
     * @param text what the hint reads
     * @param title the help's title
     * @param help the help's text
     */
    static Label create(String text, String title, String help) {
        Label link = new Label(text);
        link.setStyle(STYLE);
        link.setCursor(Cursor.HAND);
        link.underlineProperty().bind(link.hoverProperty());
        link.setOnMouseClicked(event -> showHelp(link, title, help));
        return link;
    }

    private static void showHelp(Label link, String title, String help) {
        Label text = new Label(help);
        text.setWrapText(true);
        text.setMaxWidth(HELP_WIDTH);
        Popover popover = new Popover(text);
        popover.setTitle(title);
        popover.setHeaderAlwaysVisible(true);
        popover.setDetachable(false);
        popover.setArrowLocation(Popover.ArrowLocation.TOP_LEFT);
        popover.show(link);
    }
}
