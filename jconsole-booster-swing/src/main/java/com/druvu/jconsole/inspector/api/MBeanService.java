/*
 * Copyright (c) 2026 JConsole Booster contributors.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

package com.druvu.jconsole.inspector.api;

import javax.management.MBeanServerConnection;

/**
 * Narrow interface that the MBean-inspector UI uses to reach the underlying JMX server. Hides the concrete
 * {@code MBeansTab} and {@code ProxyClient.SnapshotMBeanServerConnection} types from the inspector classes so they have
 * no hard dependency on tabs or JMX implementation details.
 */
public interface MBeanService {

    /** Direct (non-cached) connection to the MBean server. */
    MBeanServerConnection getMBeanServerConnection();

    /**
     * Attribute-caching wrapper around the MBean server connection. The returned type is opaque — callers should use it
     * as a plain {@link MBeanServerConnection}; use {@link #flushSnapshot()} to invalidate the cache.
     */
    MBeanServerConnection getSnapshotMBeanServerConnection();

    /** Flush the attribute cache backing {@link #getSnapshotMBeanServerConnection()}. */
    void flushSnapshot();
}
