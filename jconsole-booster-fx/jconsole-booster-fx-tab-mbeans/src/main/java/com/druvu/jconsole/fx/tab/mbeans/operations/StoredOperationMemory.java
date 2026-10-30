package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.core.operations.OperationStateStore;
import java.util.Optional;

/**
 * The operation memory kept on disk by the core's {@link OperationStateStore}.
 *
 * @param store the user's operation state, under the booster's home
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record StoredOperationMemory(OperationStateStore store) implements OperationMemory {

    @Override
    public Optional<String> load(String mbeanClass, String operation, String parameter) {
        return Optional.ofNullable(store.load(mbeanClass, operation, parameter));
    }

    @Override
    public void save(String mbeanClass, String operation, String parameter, String value) {
        store.save(mbeanClass, operation, parameter, value);
    }

    @Override
    public void forget(String mbeanClass, String operation, String parameter) {
        store.forget(mbeanClass, operation, parameter);
    }
}
