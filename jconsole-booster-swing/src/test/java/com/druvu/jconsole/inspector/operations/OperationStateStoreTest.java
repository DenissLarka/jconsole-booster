package com.druvu.jconsole.inspector.operations;

import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OperationStateStoreTest {

    private Path tempDir;
    private OperationStateStore store;

    private static final String CLASS = "com.example.OrderService";
    private static final String OP = "placeOrder";
    private static final String PARAM = "symbol";

    @BeforeMethod
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("operation-state-test-");
        store = new OperationStateStore(tempDir);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    @Test
    public void loadReturnsNullWhenNothingSaved() {
        Assert.assertNull(store.load(CLASS, OP, PARAM));
    }

    @Test
    public void saveThenLoadRoundTrips() {
        store.save(CLASS, OP, PARAM, "EURUSD");
        Assert.assertEquals(store.load(CLASS, OP, PARAM), "EURUSD");
    }

    @Test
    public void saveThenLoadSurvivesFreshStoreInstance() {
        store.save(CLASS, OP, PARAM, "EURUSD");
        OperationStateStore second = new OperationStateStore(tempDir);
        Assert.assertEquals(second.load(CLASS, OP, PARAM), "EURUSD");
    }

    @Test
    public void saveOverwritesPreviousValue() {
        store.save(CLASS, OP, PARAM, "EURUSD");
        store.save(CLASS, OP, PARAM, "USDCHF");
        Assert.assertEquals(store.load(CLASS, OP, PARAM), "USDCHF");
    }

    @Test
    public void blankValueDoesNotOverwriteGoodValue() {
        store.save(CLASS, OP, PARAM, "EURUSD");
        store.save(CLASS, OP, PARAM, "");
        store.save(CLASS, OP, PARAM, "   ");
        store.save(CLASS, OP, PARAM, null);
        Assert.assertEquals(store.load(CLASS, OP, PARAM), "EURUSD");
    }

    @Test
    public void forgetRemovesValue() {
        store.save(CLASS, OP, PARAM, "EURUSD");
        store.forget(CLASS, OP, PARAM);
        Assert.assertNull(store.load(CLASS, OP, PARAM));
    }

    @Test
    public void forgetIsSafeWhenKeyMissing() {
        store.forget(CLASS, OP, PARAM); // must not throw
        Assert.assertNull(store.load(CLASS, OP, PARAM));
    }

    @Test
    public void differentClassesAreIsolated() {
        store.save("com.example.A", OP, PARAM, "valueA");
        store.save("com.example.B", OP, PARAM, "valueB");
        Assert.assertEquals(store.load("com.example.A", OP, PARAM), "valueA");
        Assert.assertEquals(store.load("com.example.B", OP, PARAM), "valueB");
    }

    @Test
    public void differentOperationsAreIsolated() {
        store.save(CLASS, "opA", PARAM, "x");
        store.save(CLASS, "opB", PARAM, "y");
        Assert.assertEquals(store.load(CLASS, "opA", PARAM), "x");
        Assert.assertEquals(store.load(CLASS, "opB", PARAM), "y");
    }

    @Test
    public void differentParametersAreIsolated() {
        store.save(CLASS, OP, "a", "1");
        store.save(CLASS, OP, "b", "2");
        Assert.assertEquals(store.load(CLASS, OP, "a"), "1");
        Assert.assertEquals(store.load(CLASS, OP, "b"), "2");
    }

    @Test
    public void nullArgumentsAreSafe() {
        Assert.assertNull(store.load(null, OP, PARAM));
        Assert.assertNull(store.load(CLASS, null, PARAM));
        Assert.assertNull(store.load(CLASS, OP, null));
        store.save(null, OP, PARAM, "x"); // no-op, must not throw
        store.forget(null, OP, PARAM); // no-op, must not throw
    }
}
