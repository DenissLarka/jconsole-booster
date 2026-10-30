/**
 * JConsole Booster's Classes tab: loaded classes charted over time, the loaded, unloaded and total counts, and verbose
 * class loading on demand. The app finds it at run time through druvu-lib-loader.
 */
module com.druvu.jconsole.fx.tab.classes {
    requires com.druvu.jconsole.fx.tab;
    requires com.druvu.jconsole.fx.charts;

    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.jconsole.fx.tab.classes.ClassesTabFactory;
}
