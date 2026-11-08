package com.druvu.jconsole.fx.tab.mbeans.common;

import com.druvu.jconsole.fx.tab.TabHost;
import com.druvu.lib.fx.exec.FxExec;
import com.druvu.lib.fx.util.FxThreads;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * What every view of the tab works with: the MBean server, the app, and the background threads for the JMX calls.
 * {@link #load load} is how the views call JMX: off the FX thread, back on it with the result or the error. Once the
 * tab is closed, late results are dropped, so a disconnect does not end in a burst of error dialogs.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class TabServices implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(TabServices.class.getName());

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
        load(title, work, show, errorTitle, () -> {});
    }

    /**
     * As {@link #load(String, Callable, Consumer, String)}, then {@code finished} on the FX thread, whether the work
     * succeeded or failed; for a view that holds still while its call runs.
     */
    public <T> void load(String title, Callable<T> work, Consumer<T> show, String errorTitle, Runnable finished) {
        if (closed) {
            return;
        }
        exec.supply(title, work)
                .whenCompleteAsync(
                        (value, failure) -> {
                            if (closed) {
                                return;
                            }
                            try {
                                if (failure != null) {
                                    host.showError(errorTitle, Failures.cause(failure));
                                } else {
                                    show.accept(value);
                                }
                            } catch (RuntimeException e) {
                                // the future would swallow it silently
                                LOG.log(System.Logger.Level.ERROR, "Could not show the result of '" + title + "'", e);
                            } finally {
                                finished.run();
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
