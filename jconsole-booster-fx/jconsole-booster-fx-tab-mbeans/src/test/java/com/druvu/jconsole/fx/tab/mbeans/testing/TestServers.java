package com.druvu.jconsole.fx.tab.mbeans.testing;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * In-JVM MBean servers for the tests, each its own, never the platform's.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class TestServers {

    /** Where {@link #withGadget()} registers its gadget. */
    public static final ObjectName GADGET = name("test:type=Gadget,name=first");

    private TestServers() {}

    /** A fresh server holding one {@link Gadget} under {@link #GADGET}. */
    public static MBeanServer withGadget() {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        try {
            server.registerMBean(new Gadget(), GADGET);
        } catch (JMException e) {
            throw new IllegalStateException("Could not register the gadget", e);
        }
        return server;
    }

    public static ObjectName name(String text) {
        try {
            return new ObjectName(text);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(text, e);
        }
    }
}
