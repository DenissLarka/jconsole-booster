/**
 * JConsole Booster's time-series chart on a JavaFX Canvas: live series against time, value axes in plain numbers, byte
 * sizes or percentages, shared time ranges, and CSV export.
 */
module com.druvu.jconsole.fx.charts {
    requires transitive javafx.controls;
    requires com.druvu.jconsole.core;

    exports com.druvu.jconsole.fx.charts;
}
