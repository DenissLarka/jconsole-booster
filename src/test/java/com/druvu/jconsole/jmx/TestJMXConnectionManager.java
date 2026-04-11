package com.druvu.jconsole.jmx;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests for {@link JMXConnectionManager}. */
public class TestJMXConnectionManager {

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
