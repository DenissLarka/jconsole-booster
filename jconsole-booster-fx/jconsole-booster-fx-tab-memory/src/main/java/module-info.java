/**
 * JConsole Booster's Memory tab: heap, non-heap and each memory pool charted over time, the pools' usage bars, the
 * collectors' times, and a garbage collection on demand. The app finds it at run time through druvu-lib-loader.
 */
module com.druvu.jconsole.fx.tab.memory {
    requires com.druvu.jconsole.fx.tab;
    requires com.druvu.jconsole.fx.charts;

    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.jconsole.fx.tab.memory.MemoryTabFactory;
}
