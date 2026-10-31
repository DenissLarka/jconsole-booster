package com.druvu.jconsole.sampletarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.management.DescriptorKey;

/**
 * Management interface of {@link SampleControl}: counters, a status, the log level, operations that create load on
 * demand (memory, sleeps, CPU), and one that sends a notification.
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

    /**
     * Sets the level {@link #logMessage} prints its messages with. The operation carries descriptor fields, so the
     * MBeans tab has a descriptor to show: {@code since} on the operation, {@code legalValues} and {@code defaultValue}
     * on its parameter.
     *
     * @return the previous level
     */
    @Since("1.2")
    String changeLogLevel(@LegalValues({"ERROR", "WARN", "INFO", "DEBUG"}) @DefaultValue("INFO") String level);

    /** The level {@link #logMessage} prints with; a change sends an attribute change notification. */
    String getLogLevel();

    /** Sends a notification with this message to every listener. */
    void broadcast(String message);

    void releaseBuffers();

    void resetCounters();

    /** The release that added a feature; JMX copies it into the feature's descriptor as {@code since}. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Since {
        @DescriptorKey("since")
        String value();
    }

    /** The values a parameter accepts; JMX copies them into the parameter's descriptor as {@code legalValues}. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface LegalValues {
        @DescriptorKey("legalValues")
        String[] value();
    }

    /** A parameter's default; JMX copies it into the parameter's descriptor as {@code defaultValue}. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface DefaultValue {
        @DescriptorKey("defaultValue")
        String value();
    }
}
