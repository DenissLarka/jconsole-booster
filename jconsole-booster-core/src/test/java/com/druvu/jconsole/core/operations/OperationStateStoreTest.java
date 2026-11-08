package com.druvu.jconsole.core.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OperationStateStoreTest {

    private Path tempDir;
    private Path stateDir;
    private OperationStateStore store;

    private static final String CLASS = "com.example.OrderService";
    private static final String OP = "placeOrder";
    private static final String PARAM = "symbol";

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("operation-state-test-");
        // one level down, so a file that escapes the state folder still lands inside the temp folder, where it is seen
        stateDir = tempDir.resolve("operation-state");
        store = new OperationStateStore(stateDir);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> paths = Files.walk(tempDir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
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
        OperationStateStore second = new OperationStateStore(stateDir);
        Assert.assertEquals(second.load(CLASS, OP, PARAM), "EURUSD");
    }

    /** The files 1.1 wrote, one per bean class under its plain name, are read as they are. */
    @Test
    public void readsTheFileOfAPlainClassName() throws IOException {
        Files.createDirectories(stateDir);
        Files.writeString(stateDir.resolve(CLASS + ".properties"), OP + "." + PARAM + "=GBPUSD\n");
        Assert.assertEquals(store.load(CLASS, OP, PARAM), "GBPUSD");
    }

    /** The class name comes from the target JVM: a name that walks up the folders must not write outside it. */
    @Test
    public void classNameCannotWriteOutsideTheStateFolder() throws IOException {
        store.save("../escaped", OP, PARAM, "EURUSD");

        Assert.assertFalse(Files.exists(tempDir.resolve("escaped.properties")), "written outside the state folder");
        try (Stream<Path> files = Files.list(stateDir)) {
            Assert.assertEquals(files.count(), 1L, "the value is kept inside the state folder");
        }
        Assert.assertEquals(new OperationStateStore(stateDir).load("../escaped", OP, PARAM), "EURUSD");
    }

    @Test
    public void absoluteClassNameCannotWriteOutsideTheStateFolder() {
        Path outside = tempDir.resolve("absolute");
        store.save(outside.toString(), OP, PARAM, "EURUSD");

        Assert.assertFalse(Files.exists(Path.of(outside + ".properties")), "written outside the state folder");
    }

    @Test
    public void classNameCannotReadOutsideTheStateFolder() throws IOException {
        // the folder exists once anything was saved; through a missing folder, `..` would not resolve at all
        Files.createDirectories(stateDir);
        Files.writeString(tempDir.resolve("secret.properties"), OP + "." + PARAM + "=hunter2\n");

        Assert.assertNull(store.load("../secret", OP, PARAM), "read a file outside the state folder");
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
