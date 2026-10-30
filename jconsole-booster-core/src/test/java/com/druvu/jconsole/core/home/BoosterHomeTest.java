package com.druvu.jconsole.core.home;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class BoosterHomeTest {

    // computeRoot only joins paths and never touches the disk; these sit under the temp folder all the same.
    private static final Path EXPLICIT = Path.of(System.getProperty("java.io.tmpdir"), "explicit");
    private static final Path UNUSED_HOME = Path.of(System.getProperty("java.io.tmpdir"), "unused-home");
    private static final Path USER_HOME = Path.of(System.getProperty("java.io.tmpdir"), "alice");

    private Path tempDir;

    @AfterMethod
    public void cleanup() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> paths = Files.walk(tempDir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    @Test
    public void envVarOverrideHonored() {
        Path resolved = BoosterHome.computeRoot(EXPLICIT.toString(), UNUSED_HOME.toString());
        Assert.assertEquals(resolved, EXPLICIT);
    }

    @Test
    public void nullEnvVarFallsBackToUserHome() {
        Path resolved = BoosterHome.computeRoot(null, USER_HOME.toString());
        Assert.assertEquals(resolved, USER_HOME.resolve(".druvu.com").resolve("jconsole-booster"));
    }

    @Test
    public void emptyEnvVarFallsBackToUserHome() {
        Path resolved = BoosterHome.computeRoot("", USER_HOME.toString());
        Assert.assertEquals(resolved, USER_HOME.resolve(".druvu.com").resolve("jconsole-booster"));
    }

    @Test
    public void blankEnvVarFallsBackToUserHome() {
        Path resolved = BoosterHome.computeRoot("   ", USER_HOME.toString());
        Assert.assertEquals(resolved, USER_HOME.resolve(".druvu.com").resolve("jconsole-booster"));
    }

    @Test
    public void ensureDirIsIdempotent() throws IOException {
        tempDir = Files.createTempDirectory("booster-home-test-");
        Path target = tempDir.resolve("nested/jconsole-booster");
        Path first = BoosterHome.ensureDir(target);
        Assert.assertTrue(Files.isDirectory(first));
        // Second call on an already-existing dir must not throw.
        Path second = BoosterHome.ensureDir(target);
        Assert.assertEquals(second, first);
        Assert.assertTrue(Files.isDirectory(second));
    }
}
