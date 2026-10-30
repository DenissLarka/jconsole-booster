/**
 * The console mode of JConsole Booster: a text REPL over the core (connect, browse MBeans, invoke operations, write
 * thread dumps) for a terminal or a bastion without a display.
 */
module com.druvu.jconsole.console {
    requires com.druvu.jconsole.core;

    exports com.druvu.jconsole.console;
}
