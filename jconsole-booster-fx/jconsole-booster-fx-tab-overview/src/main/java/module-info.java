/**
 * JConsole Booster's Overview tab: heap memory, live threads, loaded classes and CPU usage of the target JVM, charted
 * side by side over one time range. The app finds it at run time through druvu-lib-loader.
 */
module com.druvu.jconsole.fx.tab.overview {
    requires com.druvu.jconsole.fx.tab;
    requires com.druvu.jconsole.fx.charts;
    requires javafx.controls;

    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.jconsole.fx.tab.overview.OverviewTabFactory;
}
