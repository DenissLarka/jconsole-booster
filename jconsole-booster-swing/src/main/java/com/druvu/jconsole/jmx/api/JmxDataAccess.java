/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.druvu.jconsole.jmx.api;

import com.druvu.jconsole.jmx.MemoryPoolProxy;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.tools.jconsole.JConsoleContext;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.Map;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * Provides access to JMX data for UI tabs without a hard dependency on {@code ProxyClient}. {@code ProxyClient}
 * implements this interface.
 */
public interface JmxDataAccess {

    // Connection state

    JConsoleContext.ConnectionState getConnectionState();

    boolean isConnected();

    boolean isDead();

    boolean hasPlatformMXBeans();

    boolean hasHotSpotDiagnosticMXBean();

    boolean hasCompilationMXBean();

    boolean isLockUsageSupported();

    // MBean server access

    MBeanServerConnection getMBeanServerConnection();

    // Standard MXBean proxies

    ClassLoadingMXBean getClassLoadingMXBean() throws IOException;

    CompilationMXBean getCompilationMXBean() throws IOException;

    MemoryMXBean getMemoryMXBean() throws IOException;

    OperatingSystemMXBean getOperatingSystemMXBean() throws IOException;

    RuntimeMXBean getRuntimeMXBean() throws IOException;

    ThreadMXBean getThreadMXBean() throws IOException;

    // HotSpot / sun extensions

    com.sun.management.OperatingSystemMXBean getSunOperatingSystemMXBean() throws IOException;

    HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() throws IOException;

    // Memory pools and GC beans

    Collection<MemoryPoolProxy> getMemoryPoolProxies() throws IOException;

    Collection<GarbageCollectorMXBean> getGarbageCollectorMXBeans() throws IOException;

    // JMX operations used by tabs

    Map<ObjectName, MBeanInfo> getMBeans(String domain) throws IOException;

    AttributeList getAttributes(ObjectName name, String[] attributes) throws IOException;

    long[] findDeadlockedThreads() throws IOException;

    void markAsDead();

    // Property change support (fires on EDT via SwingPropertyChangeSupport)

    void addPropertyChangeListener(PropertyChangeListener l);

    void addWeakPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}
