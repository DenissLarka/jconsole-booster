package com.druvu.jconsole.fx.tab;

import javafx.stage.Window;

/**
 * The app's services a tab may use. Every method is called on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public interface TabHost {

    /** The app's main window, the owner of a tab's dialogs. */
    Window window();

    /** Tells the user that something failed, with the failure's message. */
    void showError(String title, Throwable failure);

    /** A short notice that disappears by itself. */
    void notice(String message);
}
