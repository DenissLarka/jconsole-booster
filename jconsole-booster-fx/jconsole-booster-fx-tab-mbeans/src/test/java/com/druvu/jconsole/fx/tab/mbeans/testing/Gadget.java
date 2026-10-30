package com.druvu.jconsole.fx.tab.mbeans.testing;

import java.util.concurrent.atomic.AtomicLong;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

/**
 * A small standard MBean for the tests: a writable attribute, a failing one, an array, operations, notifications.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class Gadget extends NotificationBroadcasterSupport implements GadgetMBean {

    public static final String PING = "gadget.ping";

    private final AtomicLong sequence = new AtomicLong();
    private volatile int level = 3;
    private volatile String name = "Alpha";

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBroken() {
        throw new IllegalStateException("broken on purpose");
    }

    @Override
    public long[] getHistory() {
        return new long[] {1, 2, 3};
    }

    @Override
    public String echo(String text) {
        return text;
    }

    @Override
    public void rename(String newName) {
        this.name = newName;
    }

    @Override
    public void reset() {
        level = 0;
    }

    @Override
    public void ping(String message) {
        sendNotification(new Notification(PING, this, sequence.incrementAndGet(), System.currentTimeMillis(), message));
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(new String[] {PING}, Notification.class.getName(), "Pings")
        };
    }
}
