package com.druvu.jconsole.fx.tab.mbeans.tree;

import com.druvu.jconsole.core.text.Messages;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;

/**
 * What one node of the MBean tree stands for; the detail side shows a different view for each kind.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public sealed interface TreeEntry {

    /** The text of the node. */
    String label();

    /** The node's tooltip, empty for none. */
    default String tooltip() {
        return "";
    }

    /** A domain or key-property level that is not an MBean itself. */
    record Folder(String label, String tooltip) implements TreeEntry {}

    /** An MBean; its Attributes, Operations and Notifications nodes come when it is first expanded. */
    record MBean(ObjectName name, String label) implements TreeEntry {
        @Override
        public String tooltip() {
            return name.toString();
        }
    }

    /** The MBean's attribute table. */
    record Attributes(ObjectName name) implements TreeEntry {
        @Override
        public String label() {
            return Messages.ATTRIBUTES;
        }
    }

    /** The MBean's operations. */
    record Operations(ObjectName name) implements TreeEntry {
        @Override
        public String label() {
            return Messages.OPERATIONS;
        }
    }

    /** The MBean's notification buffer; the label counts what arrived while subscribed. */
    record Notifications(ObjectName name, String label) implements TreeEntry {

        /** {@code Notifications[n]} while subscribed, else {@code Notifications}. */
        public static String label(boolean subscribed, long received) {
            return subscribed ? Messages.NOTIFICATIONS + "[" + received + "]" : Messages.NOTIFICATIONS;
        }
    }

    /** One attribute: its value and its info. */
    record Attribute(ObjectName name, MBeanAttributeInfo info) implements TreeEntry {
        @Override
        public String label() {
            return info.getName();
        }

        @Override
        public String tooltip() {
            return info.getDescription() == null ? "" : info.getDescription();
        }
    }

    /** One operation: its invocation and its info. */
    record Operation(ObjectName name, MBeanOperationInfo info) implements TreeEntry {
        @Override
        public String label() {
            return info.getName();
        }

        /** {@code name(type1,type2)}. */
        @Override
        public String tooltip() {
            String types = Arrays.stream(info.getSignature())
                    .map(MBeanParameterInfo::getType)
                    .collect(Collectors.joining(","));
            return info.getName() + "(" + types + ")";
        }
    }

    /** One kind of notification the MBean emits: its info. */
    record Notification(ObjectName name, MBeanNotificationInfo info) implements TreeEntry {
        @Override
        public String label() {
            return info.getName();
        }
    }
}
