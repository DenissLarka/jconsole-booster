package com.druvu.jconsole.fx.tab.mbeans.common;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The titled blocks the detail side is made of ("Attribute values", "Operation invocation"...).
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class Sections {

    private Sections() {}

    /** A bold title above the content, which takes the height left. */
    public static VBox titled(String title, Node content) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold;");
        VBox section = new VBox(4, label, content);
        section.setPadding(new Insets(6));
        VBox.setVgrow(content, Priority.ALWAYS);
        return section;
    }
}
