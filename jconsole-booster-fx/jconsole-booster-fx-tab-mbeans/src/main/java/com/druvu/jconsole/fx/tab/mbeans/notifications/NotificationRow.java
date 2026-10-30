package com.druvu.jconsole.fx.tab.mbeans.notifications;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.management.Notification;

/**
 * One received notification as the buffer table shows it.
 *
 * @param timeStamp when it was sent, local time to the millisecond
 * @param type the notification type
 * @param sequenceNumber the emitter's sequence number
 * @param message the notification's message
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record NotificationRow(String timeStamp, String type, long sequenceNumber, String message) {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss:SSS").withZone(ZoneId.systemDefault());

    static NotificationRow of(Notification notification) {
        return new NotificationRow(
                TIME.format(Instant.ofEpochMilli(notification.getTimeStamp())),
                notification.getType(),
                notification.getSequenceNumber(),
                notification.getMessage());
    }
}
