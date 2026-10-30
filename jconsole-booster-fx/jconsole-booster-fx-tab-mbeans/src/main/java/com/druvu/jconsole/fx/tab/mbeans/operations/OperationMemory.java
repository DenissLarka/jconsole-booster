package com.druvu.jconsole.fx.tab.mbeans.operations;

import java.util.Optional;

/**
 * The last values the user invoked an operation with, per MBean class, operation and parameter, so the form comes back
 * filled in. The app keeps them on disk; tests keep them in memory.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public interface OperationMemory {

    Optional<String> load(String mbeanClass, String operation, String parameter);

    /** Keeps the value for next time; the form never passes a blank one, so a cleared field keeps a good value. */
    void save(String mbeanClass, String operation, String parameter, String value);

    void forget(String mbeanClass, String operation, String parameter);
}
