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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Refreshes the tabs of one connection on a fixed delay. Each tick flushes the connection's attribute cache, then asks
 * every tab to read its values on the tick's thread and hands the step it returns to the FX thread. A tab whose last
 * step has not run yet is skipped, so a slow screen never piles up work.
 *
 * <p>A tab that throws is switched off for the rest of the connection and reported once. A lost connection stops the
 * tick and is reported once. Every report reaches the {@link Listener} on the FX thread, and none after
 * {@link #stop()}.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@Slf4j
final class UpdateTick {

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
            // A tab whose previous step has not run yet waits for the next tick.
            if (failed.contains(tab) || !stepPending.add(tab)) {
                continue;
            }
            refresh(tab);
        }
        LocalTime at = LocalTime.now();
        fxThread.execute(() -> {
            if (!stopped) {
                listener.updated(at);
            }
        });
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
            log.warn("The {} tab failed and is no longer refreshed", tab.id(), failure);
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
            log.error("An update tick failed", unexpected);
        }
    }
}
