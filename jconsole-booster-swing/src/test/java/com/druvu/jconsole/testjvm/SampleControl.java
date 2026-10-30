package com.druvu.jconsole.testjvm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SampleControl implements SampleControlMBean {

    private final AtomicLong requestCount = new AtomicLong();
    private final AtomicLong errorCount = new AtomicLong();
    private final AtomicBoolean maintenance = new AtomicBoolean(false);
    private final List<byte[]> retainedBuffers = new ArrayList<>();

    public void recordRequest() {
        requestCount.incrementAndGet();
    }

    @Override
    public long getRequestCount() {
        return requestCount.get();
    }

    @Override
    public String getStatus() {
        long errors = errorCount.get();
        if (errors == 0) {
            return "GREEN";
        }
        return errors < 10 ? "AMBER" : "RED";
    }

    @Override
    public String toggleMaintenance(boolean enabled) {
        boolean previous = maintenance.getAndSet(enabled);
        return "maintenance " + (previous ? "ON" : "OFF") + " -> " + (enabled ? "ON" : "OFF");
    }

    @Override
    public synchronized long allocateBuffer(int megabytes) {
        if (megabytes <= 0 || megabytes > 512) {
            throw new IllegalArgumentException("megabytes must be in (0, 512]");
        }
        byte[] buf = new byte[megabytes * 1024 * 1024];
        for (int i = 0; i < buf.length; i += 4096) {
            buf[i] = 1;
        }
        retainedBuffers.add(buf);
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @Override
    public String sleepMillis(long millis) {
        if (millis < 0 || millis > 60_000) {
            throw new IllegalArgumentException("millis must be in [0, 60000]");
        }
        long start = System.currentTimeMillis();
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "interrupted after " + (System.currentTimeMillis() - start) + " ms";
        }
        return "slept " + (System.currentTimeMillis() - start) + " ms";
    }

    @Override
    public double simulateCpuLoad(double seconds) {
        if (seconds <= 0 || seconds > 30) {
            throw new IllegalArgumentException("seconds must be in (0, 30]");
        }
        long deadline = System.nanoTime() + (long) (seconds * 1_000_000_000L);
        Random rnd = new Random();
        double acc = 0;
        while (System.nanoTime() < deadline) {
            acc += Math.sqrt(rnd.nextDouble());
        }
        return acc;
    }

    @Override
    public String logMessage(String message) {
        if (maintenance.get()) {
            System.out.println("[sample] " + message);
        }
        if (message != null && message.toLowerCase().contains("error")) {
            errorCount.incrementAndGet();
        }
        return "logged " + (message == null ? 0 : message.length()) + " chars";
    }

    @Override
    public synchronized void releaseBuffers() {
        retainedBuffers.clear();
    }

    @Override
    public void resetCounters() {
        requestCount.set(0);
        errorCount.set(0);
    }
}
