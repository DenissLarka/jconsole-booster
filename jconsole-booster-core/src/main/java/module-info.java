/**
 * The JMX session of JConsole Booster without a user-interface toolkit: connecting, trust on first use, the user's
 * bookmarks and files, the MBean description markup, the messages. Every front builds on it.
 */
module com.druvu.jconsole.core {
    requires transitive java.management;
    // the HotSpot and com.sun.management views of the target JVM
    requires transitive jdk.management;
    requires com.druvu.jmxmp.common;
    requires org.slf4j;
    requires static lombok;

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
