/**
 * The contract between JConsole Booster's JavaFX app and its tabs. A tab module provides a
 * {@code com.druvu.lib.loader.ComponentFactory} of {@link com.druvu.jconsole.fx.tab.ConnectionTab}.
 */
module com.druvu.jconsole.fx.tab {
    requires transitive com.druvu.jconsole.core;
    requires transitive com.druvu.lib.loader;
    requires transitive javafx.graphics;

    exports com.druvu.jconsole.fx.tab;
}
