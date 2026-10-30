package com.druvu.jconsole.fx.tab.threads;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Two threads after one monitor: the holder keeps it while it waits to be released, the other is blocked on it. Closing
 * releases the holder and ends both.
 */
final class MonitorHolder implements AutoCloseable {

    private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);
    private static final long POLL_MILLIS = 10;

    private final Object monitor = new Object();
    private final CountDownLatch release = new CountDownLatch(1);
    private final Thread holder;
    private final Thread blocked;

    /** Starts the threads, named {@code name-holder} and {@code name-blocked}, and returns once both are in place. */
    MonitorHolder(String name) throws InterruptedException {
        CountDownLatch holding = new CountDownLatch(1);
        holder = new Thread(() -> holdUntilReleased(holding), name + "-holder");
        blocked = new Thread(() -> holdUntilReleased(holding), name + "-blocked");
        holder.start();
        if (!holding.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("The holder did not take the monitor");
        }
        blocked.start();
        awaitState(holder, Thread.State.WAITING);
        awaitState(blocked, Thread.State.BLOCKED);
    }

    Thread holder() {
        return holder;
    }

    Thread blocked() {
        return blocked;
    }

    @Override
    public void close() throws InterruptedException {
        release.countDown();
        holder.join();
        blocked.join();
    }

    private void holdUntilReleased(CountDownLatch holding) {
        synchronized (monitor) {
            holding.countDown();
            try {
                release.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void awaitState(Thread thread, Thread.State state) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (thread.getState() != state) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException(thread.getName() + " is not " + state);
            }
            Thread.sleep(POLL_MILLIS);
        }
    }
}
