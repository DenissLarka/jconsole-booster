package com.druvu.jconsole.fx.tab.threads;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Threads in a ring, each holding its own lock and waiting for the next one's: a deadlock this JVM reports. The locks
 * are interruptible, so closing the ring ends the threads.
 */
final class DeadlockRing implements AutoCloseable {

    private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);
    private static final long POLL_MILLIS = 10;

    private final List<Thread> threads = new ArrayList<>();

    private DeadlockRing() {}

    /** Starts {@code size} threads named {@code name-0}, {@code name-1}... and returns once they are deadlocked. */
    static DeadlockRing start(String name, int size) throws InterruptedException {
        List<Lock> locks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            locks.add(new ReentrantLock());
        }
        CountDownLatch allHoldTheirOwn = new CountDownLatch(size);
        DeadlockRing ring = new DeadlockRing();
        for (int i = 0; i < size; i++) {
            Lock own = locks.get(i);
            Lock next = locks.get((i + 1) % size);
            Thread thread = new Thread(() -> holdAndWait(own, next, allHoldTheirOwn), name + "-" + i);
            thread.setDaemon(true);
            ring.threads.add(thread);
            thread.start();
        }
        ring.awaitDeadlock();
        return ring;
    }

    List<String> names() {
        return threads.stream().map(Thread::getName).toList();
    }

    @Override
    public void close() throws InterruptedException {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private void awaitDeadlock() throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!deadlocked()) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("The ring did not deadlock");
            }
            Thread.sleep(POLL_MILLIS);
        }
    }

    private boolean deadlocked() {
        long[] ids = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        if (ids == null) {
            return false;
        }
        Set<Long> found = Arrays.stream(ids).boxed().collect(Collectors.toSet());
        return threads.stream().allMatch(thread -> found.contains(thread.threadId()));
    }

    private static void holdAndWait(Lock own, Lock next, CountDownLatch allHoldTheirOwn) {
        own.lock();
        try {
            allHoldTheirOwn.countDown();
            allHoldTheirOwn.await();
            // never granted while the ring stands: the next thread holds it and waits for the lock after
            next.lockInterruptibly();
            next.unlock();
        } catch (InterruptedException closed) {
            Thread.currentThread().interrupt();
        } finally {
            own.unlock();
        }
    }
}
