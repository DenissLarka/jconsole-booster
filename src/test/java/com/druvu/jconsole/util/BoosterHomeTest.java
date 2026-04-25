package com.druvu.jconsole.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class BoosterHomeTest {

    private Path tempDir;

    @AfterMethod
    public void cleanup() throws Exception {
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
    public void envVarOverrideHonored() {
        Path resolved = BoosterHome.computeRoot("/some/explicit/path", "/should/not/be/used");
        Assert.assertEquals(resolved, Paths.get("/some/explicit/path"));
    }

    @Test
    public void nullEnvVarFallsBackToUserHome() {
        Path resolved = BoosterHome.computeRoot(null, "/home/alice");
        Assert.assertEquals(resolved, Paths.get("/home/alice", ".druvu.com", "jconsole-booster"));
    }

    @Test
    public void emptyEnvVarFallsBackToUserHome() {
        Path resolved = BoosterHome.computeRoot("", "/home/alice");
        Assert.assertEquals(resolved, Paths.get("/home/alice", ".druvu.com", "jconsole-booster"));
    }

    @Test
    public void blankEnvVarFallsBackToUserHome() {
        Path resolved = BoosterHome.computeRoot("   ", "/home/alice");
        Assert.assertEquals(resolved, Paths.get("/home/alice", ".druvu.com", "jconsole-booster"));
    }

    @Test
    public void ensureDirIsIdempotent() throws Exception {
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
