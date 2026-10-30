package com.druvu.jconsole.fx.tab.mbeans;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.mbeans.common.MBeanAccess;
import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import com.druvu.jconsole.fx.tab.mbeans.notifications.NotificationBuffer;
import com.druvu.jconsole.fx.tab.mbeans.operations.OperationMemory;
import com.druvu.jconsole.fx.tab.mbeans.tree.MBeanTree;
import com.druvu.jconsole.fx.tab.mbeans.tree.TreeEntry;
import java.io.IOException;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javax.management.JMException;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What the MBeans tab shows: the MBean tree on the left, the selected node's view on the right. The tree is read once
 * and kept live by the server's registration notifications; the notification counts follow the buffer.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class MBeansView {

    private static final Logger log = LoggerFactory.getLogger(MBeansView.class);

    private final TabServices services;
    private final NotificationBuffer buffer;
    private final MBeanTree tree;
    private final DetailPane detail;
    private final SplitPane pane;
    private final NotificationListener registrations = (notification, handback) -> registrationChanged(notification);

    MBeansView(TabServices services, OperationMemory memory, NotificationBuffer buffer) {
        this.services = services;
        this.buffer = buffer;
        MBeanAccess access = services.access();
        tree = new MBeanTree(name -> services.supply("Read the metadata of " + name, () -> access.metadata(name)));
        detail = new DetailPane(services, memory, buffer, this::notificationsChanged);
        tree.onSelect(detail::show);
        pane = new SplitPane(tree.node(), detail.node());
        pane.setDividerPositions(0.3);
        SplitPane.setResizableWithParent(tree.node(), false);
        buffer.onReceived((name, row) -> Platform.runLater(() -> notificationsChanged(name)));
    }

    Node node() {
        return pane;
    }

    /** Listens to registrations first, then reads every name, so no MBean slips between the two. */
    void load() {
        MBeanAccess access = services.access();
        services.load(
                "Read the MBean names",
                () -> {
                    access.connection()
                            .addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, registrations, null, null);
                    return access.connection().queryNames(null, null);
                },
                tree::addAll,
                Messages.PROBLEM_DISPLAYING_MBEAN);
    }

    /** Stops listening to registrations; blocks, and the connection may already be gone. */
    void stopListening() {
        try {
            services.access().connection().removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, registrations);
        } catch (JMException | IOException | RuntimeException e) {
            log.debug("Could not remove the registration listener", e);
        }
    }

    /** Called on a JMX thread when an MBean comes or goes. */
    private void registrationChanged(Notification notification) {
        if (!(notification instanceof MBeanServerNotification registration)) {
            return;
        }
        ObjectName name = registration.getMBeanName();
        if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(notification.getType())) {
            services.access().forget(name);
            buffer.forget(name);
            Platform.runLater(() -> tree.remove(name));
        } else if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType())) {
            Platform.runLater(() -> tree.add(name));
        }
    }

    /** The MBean's notification buffer changed: its tree label and, when on screen, its table. */
    private void notificationsChanged(ObjectName name) {
        String label = TreeEntry.Notifications.label(buffer.isSubscribed(name), buffer.received(name));
        tree.setNotificationsLabel(name, label);
        detail.notificationsChanged(name);
    }
}
