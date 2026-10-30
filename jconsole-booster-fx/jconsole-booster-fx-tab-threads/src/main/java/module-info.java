/**
 * JConsole Booster's Threads tab: live and peak threads charted over time, the thread list with each thread's stack,
 * and deadlock detection. The app finds it at run time through druvu-lib-loader.
 */
module com.druvu.jconsole.fx.tab.threads {
    requires com.druvu.jconsole.fx.tab;
    requires com.druvu.jconsole.fx.charts;
    requires javafx.controls;

    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.jconsole.fx.tab.threads.ThreadsTabFactory;
}
