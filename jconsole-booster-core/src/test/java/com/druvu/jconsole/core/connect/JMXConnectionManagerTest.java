package com.druvu.jconsole.core.connect;

import org.testng.Assert;
import org.testng.annotations.Test;

/** The public {@link JMXConnectionManager.ConnectionResult} contract; the connections themselves: JmxmpConnectTest. */
public class JMXConnectionManagerTest {

    @Test
    public void connectionResultRecordExposesAccessors() {
        // Smoke check the public ConnectionResult contract — building one with nulls should not throw, and accessors
        // should round-trip the boolean flags. This guards against an accidental record-component reorder.
        JMXConnectionManager.ConnectionResult r =
                new JMXConnectionManager.ConnectionResult(null, null, true, false, true, false);
        Assert.assertNull(r.connector());
        Assert.assertNull(r.connection());
        Assert.assertTrue(r.hasPlatformMXBeans());
        Assert.assertFalse(r.hasHotSpotDiagnosticMXBean());
        Assert.assertTrue(r.hasCompilationMXBean());
        Assert.assertFalse(r.supportsLockUsage());
    }
}
