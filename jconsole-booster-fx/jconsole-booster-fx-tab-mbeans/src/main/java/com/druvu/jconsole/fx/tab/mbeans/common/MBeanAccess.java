package com.druvu.jconsole.fx.tab.mbeans.common;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.NotificationBroadcaster;
import javax.management.ObjectName;

/**
 * The tab's calls to the MBean server. Every method blocks on the network, so none may run on the FX thread. The
 * {@link MBeanInfo} of each MBean is read once and kept until the MBean goes away.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class MBeanAccess {

    private final MBeanServerConnection connection;
    private final Map<ObjectName, MBeanInfo> infos = new ConcurrentHashMap<>();

    /** @param connection the live connection to the target's MBean server */
    public MBeanAccess(MBeanServerConnection connection) {
        this.connection = connection;
    }

    /** The live connection, for listener registration. */
    public MBeanServerConnection connection() {
        return connection;
    }

    /** The MBean's description, read once. */
    public MBeanInfo info(ObjectName name) throws JMException, IOException {
        MBeanInfo cached = infos.get(name);
        if (cached != null) {
            return cached;
        }
        MBeanInfo info = connection.getMBeanInfo(name);
        infos.put(name, info);
        return info;
    }

    /** What the tree needs to show the MBean's children. */
    public MBeanMetadata metadata(ObjectName name) throws JMException, IOException {
        return new MBeanMetadata(info(name), isBroadcaster(name));
    }

    /** Whether the MBean emits notifications; {@code false} when the server cannot tell. */
    public boolean isBroadcaster(ObjectName name) {
        try {
            return connection.isInstanceOf(name, NotificationBroadcaster.class.getName());
        } catch (InstanceNotFoundException | IOException e) {
            return false;
        }
    }

    /** Drops what is kept about an MBean that went away. */
    public void forget(ObjectName name) {
        infos.remove(name);
    }

    public AttributeList readAttributes(ObjectName name, String... attributes) throws JMException, IOException {
        return connection.getAttributes(name, attributes);
    }

    public Object readAttribute(ObjectName name, String attribute) throws JMException, IOException {
        return connection.getAttribute(name, attribute);
    }

    public void writeAttribute(ObjectName name, Attribute attribute) throws JMException, IOException {
        connection.setAttribute(name, attribute);
    }

    public Object invoke(ObjectName name, String operation, Object[] parameters, String[] signature)
            throws JMException, IOException {
        return connection.invoke(name, operation, parameters, signature);
    }
}
