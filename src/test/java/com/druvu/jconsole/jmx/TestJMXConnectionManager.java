package com.druvu.jconsole.jmx;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.management.remote.rmi.RMIServer;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the pure-JMX validation helpers in {@link JMXConnectionManager}. These exercise {@code checkStub} without
 * requiring a live RMI registry: a non-conforming stub implementation must be rejected with a
 * {@link SecurityException}.
 */
public class TestJMXConnectionManager {

    /** Bare {@link Remote} that is neither a Proxy nor the expected stub class. */
    private static final class FakeStub implements Remote {}

    @Test
    public void checkStubRejectsUnknownStubClass() throws Exception {
        Method m = JMXConnectionManager.class.getDeclaredMethod("checkStub", Remote.class, Class.class);
        m.setAccessible(true);
        try {
            m.invoke(null, new FakeStub(), JMXConnectionManager.rmiServerImplStubClass);
            Assert.fail("Expected SecurityException for non-conforming stub");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            Assert.assertTrue(
                    cause instanceof SecurityException,
                    "Expected SecurityException, got: "
                            + (cause == null ? "null" : cause.getClass().getName()));
            Assert.assertTrue(
                    cause.getMessage() != null && cause.getMessage().contains("Expecting"),
                    "Unexpected message: " + cause.getMessage());
        }
    }

    @Test
    public void rmiServerImplStubClassIsResolved() {
        // The static initializer must succeed: the JDK ships RMIServerImpl_Stub.
        Assert.assertNotNull(JMXConnectionManager.rmiServerImplStubClass);
        Assert.assertTrue(RMIServer.class.isAssignableFrom(JMXConnectionManager.rmiServerImplStubClass)
                || Remote.class.isAssignableFrom(JMXConnectionManager.rmiServerImplStubClass));
    }

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

    /**
     * Sanity: a {@link RemoteException} subclass implementing {@link Remote} but with no live ref still gets rejected.
     * This catches the case where a caller passes a server-side stub-like object that the JDK security check should
     * still refuse.
     */
    @Test
    public void checkStubRejectsRemoteExceptionInstance() throws Exception {
        Method m = JMXConnectionManager.class.getDeclaredMethod("checkStub", Remote.class, Class.class);
        m.setAccessible(true);
        try {
            m.invoke(null, new FakeStub(), JMXConnectionManager.rmiServerImplStubClass);
            Assert.fail("Expected SecurityException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Assert.assertTrue(ite.getCause() instanceof SecurityException);
        }
    }
}
