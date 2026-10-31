package com.druvu.jconsole.sampletarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

/**
 * The sample target's control MBean: counts requests, derives a status from the error count, creates load on demand,
 * and sends notifications, so the monitoring tabs and the Notifications view have something to show.
 *
 * <p>The notifications: an attribute change when the log level changes, a heartbeat every few seconds with the request
 * count so far, and a message sent on demand with {@link #broadcast}. Their source is this object, which the MBean
 * server replaces with the MBean's name on the way to a listener.
 *
 * @author Deniss Larka <br>
 *     on 25 Apr 2026
 */
public class SampleControl extends NotificationBroadcasterSupport implements SampleControlMBean {

    /** The heartbeat's type; its user data is the request count so far. */
    public static final String HEARTBEAT = "com.druvu.testjvm.heartbeat";

    /** The type of a message sent with {@link #broadcast}. */
    public static final String MESSAGE = "com.druvu.testjvm.message";

    private final AtomicLong requestCount = new AtomicLong();
    private final AtomicLong errorCount = new AtomicLong();
    private final AtomicBoolean maintenance = new AtomicBoolean(false);
    private final List<byte[]> retainedBuffers = new ArrayList<>();
    private final AtomicReference<String> logLevel = new AtomicReference<>("INFO");
    private final AtomicLong sequence = new AtomicLong();

    /** Declares the notifications, so a JMX client can list them before it subscribes. */
    public SampleControl() {
        super(
                new MBeanNotificationInfo(
                        new String[] {AttributeChangeNotification.ATTRIBUTE_CHANGE},
                        AttributeChangeNotification.class.getName(),
                        "The log level changed"),
                new MBeanNotificationInfo(
                        new String[] {HEARTBEAT}, Notification.class.getName(), "The request count, every few seconds"),
                new MBeanNotificationInfo(
                        new String[] {MESSAGE}, Notification.class.getName(), "A message sent with broadcast"));
    }

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
            System.out.println("[sample] " + logLevel.get() + " " + message);
        }
        if (message != null && message.toLowerCase(Locale.ROOT).contains("error")) {
            errorCount.incrementAndGet();
        }
        return "logged " + (message == null ? 0 : message.length()) + " chars";
    }

    @Override
    public String changeLogLevel(String level) {
        String chosen = level == null ? "" : level.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ERROR", "WARN", "INFO", "DEBUG").contains(chosen)) {
            throw new IllegalArgumentException("level must be ERROR, WARN, INFO or DEBUG, not '" + level + "'");
        }
        String previous = logLevel.getAndSet(chosen);
        if (!previous.equals(chosen)) {
            sendNotification(new AttributeChangeNotification(
                    this,
                    sequence.incrementAndGet(),
                    System.currentTimeMillis(),
                    "Log level " + previous + " -> " + chosen,
                    "LogLevel",
                    String.class.getName(),
                    previous,
                    chosen));
        }
        return previous;
    }

    @Override
    public String getLogLevel() {
        return logLevel.get();
    }

    @Override
    public void broadcast(String message) {
        sendNotification(new Notification(MESSAGE, this, sequence.incrementAndGet(), message));
    }

    /** Sends the heartbeat: the request count so far, in the message and as the user data. */
    public void sendHeartbeat() {
        long requests = requestCount.get();
        Notification heartbeat = new Notification(HEARTBEAT, this, sequence.incrementAndGet(), requests + " requests");
        heartbeat.setUserData(requests);
        sendNotification(heartbeat);
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
