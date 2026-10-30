package com.druvu.jconsole.fx.tab.mbeans;

import com.druvu.jconsole.fx.tab.mbeans.operations.OperationMemory;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Operation memory for the tests, kept in memory so nothing reaches the user's home.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class MemoryOnlyOperationMemory implements OperationMemory {

    private final Map<String, String> values = new ConcurrentHashMap<>();

    @Override
    public Optional<String> load(String mbeanClass, String operation, String parameter) {
        return Optional.ofNullable(values.get(key(mbeanClass, operation, parameter)));
    }

    @Override
    public void save(String mbeanClass, String operation, String parameter, String value) {
        values.put(key(mbeanClass, operation, parameter), value);
    }

    @Override
    public void forget(String mbeanClass, String operation, String parameter) {
        values.remove(key(mbeanClass, operation, parameter));
    }

    private static String key(String mbeanClass, String operation, String parameter) {
        return mbeanClass + "/" + operation + "/" + parameter;
    }
}
