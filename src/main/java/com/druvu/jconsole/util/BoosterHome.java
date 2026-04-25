package com.druvu.jconsole.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the on-disk location used by JConsole Booster for user-editable configuration (e.g. connection bookmarks,
 * per-MBean operation state).
 *
 * <p>By default the directory is {@code ~/.druvu.com/jconsole-booster}. Set the {@code JCONSOLE_BOOSTER_HOME}
 * environment variable to relocate it (e.g. to a Dropbox-synced path).
 */
public final class BoosterHome {

    public static final String ENV_VAR = "JCONSOLE_BOOSTER_HOME";

    private BoosterHome() {}

    public static Path root() {
        Path root = computeRoot(System.getenv(ENV_VAR), System.getProperty("user.home"));
        return ensureDir(root);
    }

    public static Path connectionsFile() {
        return root().resolve("connections.txt");
    }

    public static Path operationStateDir() {
        return ensureDir(root().resolve("operation-state"));
    }

    static Path computeRoot(String envValue, String userHome) {
        if (envValue != null && !envValue.isBlank()) {
            return Paths.get(envValue);
        }
        return Paths.get(userHome, ".druvu.com", "jconsole-booster");
    }

    static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create JConsole Booster directory: " + dir, e);
        }
        return dir;
    }
}
