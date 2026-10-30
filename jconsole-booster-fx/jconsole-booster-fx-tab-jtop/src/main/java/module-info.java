/**
 * JConsole Booster's JTop tab: the target JVM's threads ordered by the CPU time they use. The app finds it at run time
 * through druvu-lib-loader.
 */
module com.druvu.jconsole.fx.tab.jtop {
    requires com.druvu.jconsole.fx.tab;
    requires javafx.controls;

    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.jconsole.fx.tab.jtop.JTopTabFactory;
}
