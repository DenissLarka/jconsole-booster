/**
 * The contract between JConsole Booster's JavaFX app and its tabs. A tab module provides a
 * {@code com.druvu.lib.loader.ComponentFactory} of {@link com.druvu.jconsole.fx.tab.ConnectionTab}. Beside the
 * contract, the helpers every tab shares: a read that hands a lost connection over as such, a user's JMX call off the
 * FX thread, the details grid.
 */
module com.druvu.jconsole.fx.tab {
    requires transitive com.druvu.jconsole.core;
    requires transitive com.druvu.lib.loader;
    requires transitive javafx.graphics;
    requires javafx.controls;

    exports com.druvu.jconsole.fx.tab;
}
