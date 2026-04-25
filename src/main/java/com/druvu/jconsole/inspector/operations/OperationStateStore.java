package com.druvu.jconsole.inspector.operations;

import com.druvu.jconsole.util.BoosterHome;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Per-MBean-class persistence of last-used operation parameter values, so the
 * Operations tab can pre-fill widgets on the next visit.
 *
 * <p>Values are stored under {@link BoosterHome#operationStateDir()} as
 * {@code <fully.qualified.MBeanClassName>.properties}. Property keys use the
 * format {@code <operationName>.<parameterName>}. Keying off the bean class
 * (not its {@link javax.management.ObjectName}) means all instances of the
 * same bean share the same set of last-used values — usually the right
 * default when debugging.
 *
 * <p>Implementation notes / bug fixes from the prior implementation
 * ({@code AutoParameters} in jconsole-ojdk8):
 * <ul>
 *   <li>Loaded {@link Properties} are cached per class name for the life of
 *       the store, so widget construction does not re-read the file.</li>
 *   <li>{@link #save} only happens after a successful invoke (caller's
 *       responsibility) — never on every render — and refuses to overwrite
 *       an existing value with a blank one.</li>
 *   <li>I/O failures log a warning and degrade gracefully; they never throw
 *       to the UI thread.</li>
 * </ul>
 */
@Slf4j
public final class OperationStateStore {

    private final Path stateDir;
    private final ConcurrentMap<String, Properties> cache = new ConcurrentHashMap<>();

    public OperationStateStore() {
        this(BoosterHome.operationStateDir());
    }

    /** Test seam: pin the store to an explicit directory. */
    OperationStateStore(Path stateDir) {
        this.stateDir = stateDir;
    }

    /** @return the last-saved value or {@code null} if none. */
    public String load(String mbeanClassName, String operationName, String parameterName) {
        if (mbeanClassName == null || operationName == null || parameterName == null) {
            return null;
        }
        Properties props = propsFor(mbeanClassName);
        return props.getProperty(key(operationName, parameterName));
    }

    /**
     * Records the given value for next time. Blank or {@code null} values are
     * no-ops so a successful invoke with a cleared field doesn't wipe a
     * previously-good value (the user may have temporarily blanked it).
     */
    public void save(String mbeanClassName, String operationName, String parameterName, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (mbeanClassName == null || operationName == null || parameterName == null) {
            return;
        }
        Properties props = propsFor(mbeanClassName);
        props.setProperty(key(operationName, parameterName), value);
        flush(mbeanClassName, props);
    }

    /** Drops the saved value (used by the "Forget this value" right-click action). */
    public void forget(String mbeanClassName, String operationName, String parameterName) {
        if (mbeanClassName == null || operationName == null || parameterName == null) {
            return;
        }
        Properties props = propsFor(mbeanClassName);
        if (props.remove(key(operationName, parameterName)) != null) {
            flush(mbeanClassName, props);
        }
    }

    private static String key(String operationName, String parameterName) {
        return operationName + "." + parameterName;
    }

    private Properties propsFor(String mbeanClassName) {
        return cache.computeIfAbsent(mbeanClassName, this::readFromDisk);
    }

    private Properties readFromDisk(String mbeanClassName) {
        Properties props = new Properties();
        Path file = stateDir.resolve(mbeanClassName + ".properties");
        if (!Files.exists(file)) {
            return props;
        }
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException ex) {
            log.warn("Could not read operation state file {}: {}", file, ex.getMessage());
        }
        return props;
    }

    private void flush(String mbeanClassName, Properties props) {
        Path file = stateDir.resolve(mbeanClassName + ".properties");
        try {
            Files.createDirectories(stateDir);
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "JConsole Booster — last-used operation parameter values");
            }
        } catch (IOException ex) {
            log.warn("Could not write operation state file {}: {}", file, ex.getMessage());
        }
    }
}
