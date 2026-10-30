package com.druvu.jconsole.fx.tab.mbeans;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.mbeans.attributes.AttributesView;
import com.druvu.jconsole.fx.tab.mbeans.common.Sections;
import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import com.druvu.jconsole.fx.tab.mbeans.info.InfoRows;
import com.druvu.jconsole.fx.tab.mbeans.info.InfoView;
import com.druvu.jconsole.fx.tab.mbeans.notifications.NotificationBuffer;
import com.druvu.jconsole.fx.tab.mbeans.notifications.NotificationsView;
import com.druvu.jconsole.fx.tab.mbeans.operations.OperationMemory;
import com.druvu.jconsole.fx.tab.mbeans.operations.OperationsView;
import com.druvu.jconsole.fx.tab.mbeans.tree.TreeEntry;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;

/**
 * The right side of the tab: the view of the node selected in the tree. An MBean shows its info; Attributes the value
 * table; one attribute its value and info; Operations the invocation rows; one operation its row and info;
 * Notifications the buffer; one notification its info.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class DetailPane {

    private final TabServices services;
    private final OperationMemory memory;
    private final NotificationBuffer buffer;
    private final Consumer<ObjectName> notificationsChanged;
    private final BorderPane pane = new BorderPane();
    /** Counts the selections, so a slow read for an earlier one does not replace the current view. */
    private long selection;
    /** The notification buffer on screen, if any. */
    private NotificationsView notifications;

    DetailPane(
            TabServices services,
            OperationMemory memory,
            NotificationBuffer buffer,
            Consumer<ObjectName> notificationsChanged) {
        this.services = services;
        this.memory = memory;
        this.buffer = buffer;
        this.notificationsChanged = notificationsChanged;
    }

    Node node() {
        return pane;
    }

    /** Shows the view of the entry; nothing for a folder or no selection. */
    void show(TreeEntry entry) {
        selection++;
        notifications = null;
        pane.setCenter(null);
        switch (entry) {
            case null -> {
                // nothing selected
            }
            case TreeEntry.Folder _ -> {
                // a folder has nothing to show
            }
            case TreeEntry.MBean mbean ->
                withInfo(mbean.name(), info -> new InfoView(InfoRows.forMBean(mbean.name(), info)).node());
            case TreeEntry.Attributes attributes ->
                withInfo(
                        attributes.name(),
                        info -> attributes(
                                attributes.name(), List.of(info.getAttributes()), Messages.ATTRIBUTE_VALUES));
            case TreeEntry.Attribute attribute ->
                pane.setCenter(stacked(
                        attributes(attribute.name(), List.of(attribute.info()), Messages.ATTRIBUTE_VALUE),
                        new InfoView(InfoRows.forAttribute(attribute.info())).node()));
            case TreeEntry.Operations operations ->
                withInfo(
                        operations.name(),
                        info -> operations(operations.name(), info.getClassName(), List.of(info.getOperations())));
            case TreeEntry.Operation operation ->
                withInfo(
                        operation.name(),
                        info -> stacked(
                                operations(operation.name(), info.getClassName(), List.of(operation.info())),
                                new InfoView(InfoRows.forOperation(operation.info())).node()));
            case TreeEntry.Notifications buffered -> {
                notifications = new NotificationsView(services, buffer, buffered.name(), notificationsChanged);
                pane.setCenter(Sections.titled(Messages.NOTIFICATION_BUFFER, notifications.node()));
            }
            case TreeEntry.Notification notification ->
                pane.setCenter(new InfoView(InfoRows.forNotification(notification.info())).node());
        }
    }

    /** Refreshes the notification buffer on screen when it is the MBean's. */
    void notificationsChanged(ObjectName name) {
        if (notifications != null && notifications.shows(name)) {
            notifications.refresh();
        }
    }

    /** Reads the MBean's info off the FX thread, then shows its view, unless the selection moved on meanwhile. */
    private void withInfo(ObjectName name, Function<MBeanInfo, Node> view) {
        long requested = selection;
        services.load(
                "Read the info of " + name,
                () -> services.access().info(name),
                info -> {
                    if (requested == selection) {
                        pane.setCenter(view.apply(info));
                    }
                },
                Messages.PROBLEM_DISPLAYING_MBEAN);
    }

    private Node attributes(ObjectName name, List<MBeanAttributeInfo> attributes, String title) {
        AttributesView view = new AttributesView(services, name, attributes);
        view.load();
        return Sections.titled(title, view.node());
    }

    private Node operations(ObjectName name, String mbeanClass, List<MBeanOperationInfo> operations) {
        OperationsView view = new OperationsView(services, memory, name, mbeanClass, operations);
        return Sections.titled(Messages.OPERATION_INVOCATION, view.node());
    }

    /** The feature's working view above its info. */
    private static Node stacked(Node top, Node info) {
        SplitPane split = new SplitPane(top, info);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.4);
        return split;
    }
}
