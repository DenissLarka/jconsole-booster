package com.druvu.jconsole.fx.tab.mbeans.common;

import javax.management.MBeanInfo;

/**
 * What the tree needs to show under an MBean: its attributes, operations and notifications.
 *
 * @param info the MBean's description
 * @param broadcaster whether the MBean emits notifications, which earns it a Notifications node
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record MBeanMetadata(MBeanInfo info, boolean broadcaster) {}
