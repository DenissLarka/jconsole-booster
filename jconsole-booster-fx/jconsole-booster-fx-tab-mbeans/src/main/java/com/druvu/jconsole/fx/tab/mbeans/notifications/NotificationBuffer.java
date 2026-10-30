package com.druvu.jconsole.fx.tab.mbeans.notifications;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The notifications received per MBean while the user is subscribed, newest first. Unsubscribing keeps the buffer;
 * subscribing again starts it empty. Thread-safe: notifications arrive on JMX threads, the screen reads on the FX
 * thread. Subscribing and unsubscribing call the server, so not on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class NotificationBuffer {

    private static final Logger log = LoggerFactory.getLogger(NotificationBuffer.class);

    private final MBeanServerConnection connection;
    private final Map<ObjectName, Subscription> subscriptions = new ConcurrentHashMap<>();
    private volatile BiConsumer<ObjectName, NotificationRow> onReceived = (name, row) -> {};

    public NotificationBuffer(MBeanServerConnection connection) {
        this.connection = connection;
    }

    /** Tells {@code listener} of each notification received, on the JMX thread that delivered it. */
    public void onReceived(BiConsumer<ObjectName, NotificationRow> listener) {
        this.onReceived = listener;
    }

    /** Starts listening to the MBean with an empty buffer; nothing changes when already subscribed. */
    public void subscribe(ObjectName name) throws InstanceNotFoundException, IOException {
        Subscription subscription = subscriptions.computeIfAbsent(name, Subscription::new);
        if (subscription.isActive()) {
            return;
        }
        subscription.restart();
        try {
            connection.addNotificationListener(name, subscription, null, null);
        } catch (InstanceNotFoundException | IOException | RuntimeException e) {
            subscription.stop();
            throw e;
        }
    }

    /** Stops listening; the received notifications stay. */
    public void unsubscribe(ObjectName name) throws IOException {
        Subscription subscription = subscriptions.get(name);
        if (subscription == null || !subscription.isActive()) {
            return;
        }
        subscription.stop();
        try {
            connection.removeNotificationListener(name, subscription, null, null);
        } catch (InstanceNotFoundException | ListenerNotFoundException e) {
            log.debug("The listener on {} was already gone", name, e);
        }
    }

    public boolean isSubscribed(ObjectName name) {
        Subscription subscription = subscriptions.get(name);
        return subscription != null && subscription.isActive();
    }

    /** How many notifications arrived since the subscription started or the buffer was cleared. */
    public long received(ObjectName name) {
        Subscription subscription = subscriptions.get(name);
        return subscription == null ? 0 : subscription.received();
    }

    /** The received notifications, newest first. */
    public List<NotificationRow> rows(ObjectName name) {
        Subscription subscription = subscriptions.get(name);
        return subscription == null ? List.of() : subscription.rows();
    }

    public void clear(ObjectName name) {
        Subscription subscription = subscriptions.get(name);
        if (subscription != null) {
            subscription.clear();
        }
    }

    /** The MBean went away, and its listener with it. */
    public void forget(ObjectName name) {
        Subscription subscription = subscriptions.remove(name);
        if (subscription != null) {
            subscription.stop();
        }
    }

    /** Removes every listener, for the tab's disposal; failures are only logged, the connection may be gone. */
    public void unsubscribeAll() {
        for (ObjectName name : List.copyOf(subscriptions.keySet())) {
            try {
                unsubscribe(name);
            } catch (IOException | RuntimeException e) {
                log.debug("Could not unsubscribe from {}", name, e);
            }
        }
        subscriptions.clear();
    }

    /** The listener on one MBean and what it received. */
    private final class Subscription implements NotificationListener {

        private final ObjectName name;
        private final Deque<NotificationRow> rows = new ArrayDeque<>();
        private long received;
        private boolean active;

        Subscription(ObjectName name) {
            this.name = name;
        }

        @Override
        public void handleNotification(Notification notification, Object handback) {
            NotificationRow row = NotificationRow.of(notification);
            synchronized (this) {
                if (!active) {
                    return;
                }
                rows.addFirst(row);
                received++;
            }
            onReceived.accept(name, row);
        }

        synchronized boolean isActive() {
            return active;
        }

        synchronized void restart() {
            clear();
            active = true;
        }

        synchronized void stop() {
            active = false;
        }

        synchronized void clear() {
            rows.clear();
            received = 0;
        }

        synchronized long received() {
            return received;
        }

        synchronized List<NotificationRow> rows() {
            return List.copyOf(rows);
        }
    }
}
