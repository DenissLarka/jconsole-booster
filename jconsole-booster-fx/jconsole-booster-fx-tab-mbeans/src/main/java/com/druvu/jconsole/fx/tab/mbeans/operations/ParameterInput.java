package com.druvu.jconsole.fx.tab.mbeans.operations;

import javafx.scene.Node;

/**
 * The input of one operation parameter: a text field, a list, a date picker, a text area, a file chooser or a check
 * box.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public interface ParameterInput {

    Node node();

    /** What the user entered: text, or the chosen file's content. */
    Object value();

    /** Whether the entered value is kept for next time; a file is not. */
    boolean isRemembered();

    /** Offers "Forget this value" in the input's context menu. */
    void onForget(Runnable forget);
}
