package com.druvu.jconsole.testjvm;

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
