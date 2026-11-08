/**
 * The JMX session of JConsole Booster without a user-interface toolkit: connecting, trust on first use, the user's
 * bookmarks and files, the MBean description markup, the messages. Every front builds on it.
 */
module com.druvu.jconsole.core {
    requires transitive java.management;
    // the HotSpot and com.sun.management views of the target JVM
    requires transitive jdk.management;
    // RMI, the JDK's own connector: its server stub is read before a password is sent
    requires java.management.rmi;
    // finds the protocol transports at run time
    requires com.druvu.lib.loader;

    exports com.druvu.jconsole.core.bookmarks;
    exports com.druvu.jconsole.core.connect;
    exports com.druvu.jconsole.core.home;
    exports com.druvu.jconsole.core.launch;
    exports com.druvu.jconsole.core.mbeans;
    exports com.druvu.jconsole.core.operations;
    exports com.druvu.jconsole.core.text;
    exports com.druvu.jconsole.core.trust;
    exports com.druvu.jconsole.core.vm;
}
