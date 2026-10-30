/**
 * JConsole Booster's VM Summary tab: the target JVM's runtime, threads, classes, memory, collectors, operating system
 * and arguments on one page. The app finds it at run time through druvu-lib-loader.
 */
module com.druvu.jconsole.fx.tab.summary {
    requires com.druvu.jconsole.fx.tab;
    // javafx.controls and the AtlantaFX style classes
    requires com.druvu.lib.fx;

    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.jconsole.fx.tab.summary.SummaryTabFactory;
}
