package com.druvu.jconsole.app.session;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Refreshes the tabs of one connection on a fixed delay. Each tick flushes the connection's attribute cache, then asks
 * every tab to read its values on the tick's thread and hands the step it returns to the FX thread. A tab whose last
 * step has not run yet is skipped, so a slow screen never piles up work. A tab that rests while hidden
 * ({@link ConnectionTab#refreshesWhileHidden()}) is read only while it is the one shown.
 *
 * <p>A tab that throws is switched off for the rest of the connection and reported once. A lost connection stops the
 * tick and is reported once. Every report reaches the {@link Listener} on the FX thread, and none after
 * {@link #stop()}.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class UpdateTick {

    private static final System.Logger LOG = System.getLogger(UpdateTick.class.getName());

    /** What the tick reports. Every method runs on the FX thread. */
    interface Listener {

        /** A tab failed and is no longer refreshed. */
        void tabFailed(ConnectionTab tab, RuntimeException failure);

        /** The connection is gone; the tick has stopped. */
        void connectionLost(Exception cause);

        /** A tick has handed every tab's step over. */
        void updated(LocalTime at);
    }

    private final Runnable flush;
    private final List<ConnectionTab> tabs;
    private final Executor fxThread;
    private final Listener listener;

    private final Set<ConnectionTab> stepPending = ConcurrentHashMap.newKeySet();
    private final Set<ConnectionTab> failed = ConcurrentHashMap.newKeySet();
    // stopped: by the owner, silences every report; lost: by the tick itself, ends the ticking
    private volatile boolean stopped;
    private volatile boolean lost;
    // the tab the user sees, set on the FX thread, read on the tick's
    private volatile ConnectionTab shown;
    private ScheduledExecutorService scheduler;

    /**
     * @param flush empties the connection's attribute cache, so each tick reads fresh values
     * @param tabs the tabs to refresh, in order
     * @param fxThread runs a step on the FX thread
     * @param listener told about failures and progress
     */
    UpdateTick(Runnable flush, List<ConnectionTab> tabs, Executor fxThread, Listener listener) {
        this.flush = Objects.requireNonNull(flush, "flush");
        this.tabs = List.copyOf(tabs);
        this.fxThread = Objects.requireNonNull(fxThread, "fxThread");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /** Ticks now, then every {@code interval} after the previous tick ended, on a daemon thread. */
    void start(Duration interval) {
        scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("jcb-update-tick").daemon().factory());
        scheduler.scheduleWithFixedDelay(this::tickSafely, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * The tab the user now sees. A tab that rests while hidden is read at once, so it does not show a stale table until
     * the next tick. Call on the FX thread.
     */
    void shown(ConnectionTab tab) {
        shown = tab;
        if (tab == null || tab.refreshesWhileHidden() || scheduler == null || stopped || lost) {
            return;
        }
        try {
            scheduler.execute(() -> {
                // fresh values, as a tick reads them
                flush.run();
                refreshIfDue(tab);
            });
        } catch (RejectedExecutionException tickEnded) {
            // the connection was lost or closed meanwhile: nothing to read
            LOG.log(System.Logger.Level.DEBUG, "The " + tab.id() + " tab was shown after the tick ended");
        }
    }

    /** Ends the ticking; the listener hears nothing more. Call on the FX thread. */
    void stop() {
        stopped = true;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /** One tick: refreshes every tab that is due. */
    void tick() {
        if (stopped || lost) {
            return;
        }
        flush.run();
        for (ConnectionTab tab : tabs) {
            if (stopped || lost) {
                return;
            }
            refreshIfDue(tab);
        }
        LocalTime at = LocalTime.now();
        fxThread.execute(() -> {
            if (!stopped) {
                listener.updated(at);
            }
        });
    }

    /** Reads the tab unless it failed, rests while another tab is shown, or its previous step has not run yet. */
    private void refreshIfDue(ConnectionTab tab) {
        if (stopped || lost || failed.contains(tab)) {
            return;
        }
        if (!tab.refreshesWhileHidden() && tab != shown) {
            return;
        }
        // A tab whose previous step has not run yet waits for the next tick.
        if (!stepPending.add(tab)) {
            return;
        }
        refresh(tab);
    }

    private void refresh(ConnectionTab tab) {
        Runnable step;
        try {
            step = Objects.requireNonNull(tab.refresh(), "the refresh step of " + tab.id());
        } catch (IOException connectionGone) {
            stepPending.remove(tab);
            lose(connectionGone);
            return;
        } catch (RuntimeException failure) {
            stepPending.remove(tab);
            if (ConnectionLoss.isLoss(failure)) {
                lose(failure);
            } else {
                fail(tab, failure);
            }
            return;
        }
        fxThread.execute(() -> runStep(tab, step));
    }

    private void runStep(ConnectionTab tab, Runnable step) {
        try {
            if (!stopped && !failed.contains(tab)) {
                step.run();
            }
        } catch (RuntimeException failure) {
            fail(tab, failure);
        } finally {
            stepPending.remove(tab);
        }
    }

    private void fail(ConnectionTab tab, RuntimeException failure) {
        if (failed.add(tab)) {
            LOG.log(System.Logger.Level.WARNING, "The " + tab.id() + " tab failed and is no longer refreshed", failure);
            fxThread.execute(() -> {
                if (!stopped) {
                    listener.tabFailed(tab, failure);
                }
            });
        }
    }

    private void lose(Exception cause) {
        if (stopped || lost) {
            return;
        }
        lost = true;
        if (scheduler != null) {
            scheduler.shutdown();
        }
        fxThread.execute(() -> {
            if (!stopped) {
                listener.connectionLost(cause);
            }
        });
    }

    // An exception escaping a scheduled task cancels it silently; the tick must keep going.
    private void tickSafely() {
        try {
            tick();
        } catch (RuntimeException unexpected) {
            LOG.log(System.Logger.Level.ERROR, "An update tick failed", unexpected);
        }
    }
}
