/**
 * The JMXMP protocol for JConsole Booster: adaptive TLS (the server's profiles mirrored), trust on first use, and the
 * refusal to send credentials in the clear. The core finds it at run time through druvu-lib-loader.
 */
module com.druvu.jconsole.protocol.jmxmp {
    requires com.druvu.jconsole.core;
    requires com.druvu.jmxmp.common;
    // the JMXMP connector that JMXConnectorFactory finds
    requires com.druvu.jmxmp.client;
    requires com.druvu.lib.loader;

    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.jconsole.protocol.jmxmp.JmxmpTransportFactory;
}
