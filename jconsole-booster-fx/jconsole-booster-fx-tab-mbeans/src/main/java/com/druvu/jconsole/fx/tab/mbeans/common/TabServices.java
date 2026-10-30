package com.druvu.jconsole.fx.tab.mbeans.common;

import com.druvu.jconsole.fx.tab.TabHost;
import com.druvu.lib.fx.exec.FxExec;
import com.druvu.lib.fx.util.FxThreads;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What every view of the tab works with: the MBean server, the app, and the background threads for the JMX calls.
 * {@link #load load} is how the views call JMX: off the FX thread, back on it with the result or the error. Once the
 * tab is closed, late results are dropped, so a disconnect does not end in a burst of error dialogs.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class TabServices implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TabServices.class);

    private final MBeanAccess access;
    private final TabHost host;
    private final FxExec exec = new FxExec();
    private volatile boolean closed;

    /**
     * @param access the MBean server
     * @param host the app: its window, error dialogs and notices
     */
    public TabServices(MBeanAccess access, TabHost host) {
        this.access = access;
        this.host = host;
    }

    public MBeanAccess access() {
        return access;
    }

    public TabHost host() {
        return host;
    }

    /** Runs {@code work} on a virtual thread. */
    public <T> CompletableFuture<T> supply(String title, Callable<T> work) {
        return exec.supply(title, work);
    }

    /**
     * Runs {@code work} off the FX thread, then {@code show} with its result on the FX thread; a failure goes to the
     * app's error dialog under {@code errorTitle}.
     */
    public <T> void load(String title, Callable<T> work, Consumer<T> show, String errorTitle) {
        if (closed) {
            return;
        }
        exec.supply(title, work)
                .whenCompleteAsync(
                        (value, failure) -> {
                            if (closed) {
                                return;
                            }
                            if (failure != null) {
                                host.showError(errorTitle, Failures.cause(failure));
                                return;
                            }
                            try {
                                show.accept(value);
                            } catch (RuntimeException e) {
                                // the future would swallow it silently
                                log.error("Could not show the result of '{}'", title, e);
                            }
                        },
                        FxThreads.fxExecutor());
    }

    /** Stops the background threads and drops the results still to come. */
    @Override
    public void close() {
        closed = true;
        exec.close();
    }
}
