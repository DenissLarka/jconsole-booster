package com.druvu.jconsole.app.window;

/**
 * The main window's title: the app's name, and the connection's name while connected.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class WindowTitle {

    static final String APP_NAME = "JConsole Booster";

    private WindowTitle() {}

    static String connectedTo(String connectionName) {
        return APP_NAME + " — " + connectionName;
    }

    static String notConnected() {
        return APP_NAME + " (disconnected)";
    }
}
