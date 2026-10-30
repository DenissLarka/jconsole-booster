/**
 * JConsole Booster's MBeans tab for the JavaFX app: the MBean tree, attributes with editors and viewers, operations
 * with the description-markup widgets and downloads, notifications and info. The app finds it at run time through
 * druvu-lib-loader.
 */
module com.druvu.jconsole.fx.tab.mbeans {
    requires com.druvu.jconsole.fx.tab;
    requires com.druvu.lib.fx;
    requires javafx.controls;
    // java.awt.Desktop: opens a returned file with the OS application
    requires java.desktop;
    requires org.slf4j;

    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.jconsole.fx.tab.mbeans.MBeansTabFactory;
}
