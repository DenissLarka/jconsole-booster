/**
 * JConsole Booster, the JavaFX app: one window on one JVM connection, the connect and certificate dialogs, bookmarks,
 * the tabs found at run time, and the console mode handed over to the console module.
 *
 * <p>JavaFX comes from the JDK (a JavaFX-bundled build such as Azul Zulu FX).
 */
module com.druvu.jconsole.app {
    requires com.druvu.jconsole.fx.tab;
    requires com.druvu.jconsole.console;
    requires com.druvu.lib.fx;
    // java.awt.Desktop opens the bookmarks file in the user's editor; the kit's macOS menu hooks need it too
    requires java.desktop;
    requires org.slf4j;
    requires static lombok;

    // javafx.graphics creates the Application subclass reflectively
    exports com.druvu.jconsole.app to
            javafx.graphics;
}
