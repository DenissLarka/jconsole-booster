package com.druvu.jconsole.fx.tab.mbeans.common;

import com.druvu.jconsole.core.mbeans.MBeanExceptions;
import java.util.concurrent.CompletionException;

/**
 * Finds the failure the user should see behind the wrappers of a background task and of JMX.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class Failures {

    private Failures() {}

    /** The MBean's own exception behind a {@link CompletionException} and JMX's wrappers. */
    public static Throwable cause(Throwable failure) {
        Throwable unwrapped = failure;
        if (unwrapped instanceof CompletionException && unwrapped.getCause() != null) {
            unwrapped = unwrapped.getCause();
        }
        return MBeanExceptions.actualCause(unwrapped);
    }
}
