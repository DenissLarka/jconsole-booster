package com.druvu.jconsole.sampletarget;

/**
 * Management interface of {@link SampleControl}: counters, a status, and operations that create load on demand (memory,
 * sleeps, CPU).
 *
 * @author Deniss Larka <br>
 *     on 25 Apr 2026
 */
public interface SampleControlMBean {

    long getRequestCount();

    String getStatus();

    String toggleMaintenance(boolean enabled);

    long allocateBuffer(int megabytes);

    String sleepMillis(long millis);

    double simulateCpuLoad(double seconds);

    String logMessage(String message);

    void releaseBuffers();

    void resetCounters();
}
