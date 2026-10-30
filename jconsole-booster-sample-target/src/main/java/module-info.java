/**
 * A sample target JVM for JConsole Booster: a secured JMXMP endpoint with a few MBeans and synthetic load. The JMXMP
 * server is found at run time through {@code JMXConnectorServerProvider}.
 */
module com.druvu.jconsole.sampletarget {
    requires java.management;

    // JMX introspects the MBean interfaces reflectively.
    exports com.druvu.jconsole.sampletarget;
}
