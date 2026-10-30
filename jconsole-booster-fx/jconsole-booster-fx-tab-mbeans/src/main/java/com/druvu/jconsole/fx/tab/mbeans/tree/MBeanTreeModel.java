package com.druvu.jconsole.fx.tab.mbeans.tree;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.mbeans.common.MBeanMetadata;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javafx.scene.control.TreeItem;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

/**
 * The nodes of the MBean tree, kept in step with the server: MBeans come and go one by one, and the folders above an
 * MBean exist as long as something lives below them. Children are sorted by label, with an MBean's Attributes,
 * Operations and Notifications nodes first. Call on the FX thread once the tree is on screen.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class MBeanTreeModel {

    private static final Comparator<TreeEntry> ORDER =
            Comparator.comparingInt(MBeanTreeModel::rank).thenComparing(TreeEntry::label);

    private final TreeItem<TreeEntry> root = new MBeanTreeItem(new TreeEntry.Folder(Messages.MBEANS, ""));
    /** Every folder and MBean node by its {@link MBeanPath.Step#id() id}. */
    private final Map<String, MBeanTreeItem> items = new HashMap<>();

    /** The hidden root; its children are the domains. */
    public TreeItem<TreeEntry> root() {
        return root;
    }

    /** The node of a registered MBean. */
    public Optional<TreeItem<TreeEntry>> find(ObjectName name) {
        MBeanTreeItem item = items.get(MBeanPath.of(name).leaf().id());
        if (item == null || !(item.getValue() instanceof TreeEntry.MBean)) {
            return Optional.empty();
        }
        return Optional.of(item);
    }

    /**
     * Adds the MBean and the folders above it. Adding an MBean twice changes nothing; a folder that bears the MBean's
     * name becomes the MBean.
     *
     * @return the MBean's node
     */
    public TreeItem<TreeEntry> add(ObjectName name) {
        MBeanPath path = MBeanPath.of(name);
        TreeItem<TreeEntry> parent = root;
        for (MBeanPath.Step step : path.steps().subList(0, path.steps().size() - 1)) {
            parent = folder(parent, step);
        }
        MBeanPath.Step leaf = path.leaf();
        TreeEntry.MBean entry = new TreeEntry.MBean(name, leaf.label());
        MBeanTreeItem item = items.get(leaf.id());
        if (item == null) {
            item = new MBeanTreeItem(entry);
            insertSorted(parent, item);
            items.put(leaf.id(), item);
        } else if (!(item.getValue() instanceof TreeEntry.MBean)) {
            item.setValue(entry);
        }
        return item;
    }

    /**
     * Removes the MBean's node and every folder left empty above it. A node that still leads to other MBeans stays, as
     * a folder.
     */
    public void remove(ObjectName name) {
        MBeanPath path = MBeanPath.of(name);
        MBeanTreeItem item = items.get(path.leaf().id());
        if (item == null || !(item.getValue() instanceof TreeEntry.MBean)) {
            return;
        }
        item.getChildren().removeIf(child -> isMetadata(child.getValue()));
        item.setMetadataRequested(false);
        if (!item.getChildren().isEmpty()) {
            item.setValue(new TreeEntry.Folder(path.leaf().label(), path.leaf().tooltip()));
            return;
        }
        List<MBeanPath.Step> steps = path.steps();
        for (int level = steps.size() - 1; level >= 0; level--) {
            String id = steps.get(level).id();
            MBeanTreeItem node = items.get(id);
            boolean emptyFolder = node != null
                    && node.getChildren().isEmpty()
                    && (node == item || node.getValue() instanceof TreeEntry.Folder);
            if (!emptyFolder) {
                return;
            }
            node.getParent().getChildren().remove(node);
            items.remove(id);
        }
    }

    /**
     * Marks the MBean node's children as asked for.
     *
     * @return whether the caller should read them: false for a node that is not an MBean or was asked for already
     */
    public boolean requestMetadata(TreeItem<TreeEntry> item) {
        if (item instanceof MBeanTreeItem node
                && node.getValue() instanceof TreeEntry.MBean
                && !node.isMetadataRequested()) {
            node.setMetadataRequested(true);
            return true;
        }
        return false;
    }

    /** Lets a later expansion ask again after the read failed. */
    public void metadataFailed(TreeItem<TreeEntry> item) {
        if (item instanceof MBeanTreeItem node) {
            node.setMetadataRequested(false);
        }
    }

    /** Puts the Attributes, Operations and Notifications nodes under the MBean, unless it went away meanwhile. */
    public void addMetadata(TreeItem<TreeEntry> item, MBeanMetadata metadata) {
        if (!(item.getValue() instanceof TreeEntry.MBean mbean)
                || find(mbean.name()).orElse(null) != item) {
            return;
        }
        ObjectName name = mbean.name();
        MBeanInfo info = metadata.info();
        List<TreeItem<TreeEntry>> groups = new ArrayList<>();
        if (info.getAttributes().length > 0) {
            groups.add(group(
                    new TreeEntry.Attributes(name),
                    Arrays.stream(info.getAttributes()).map(attribute -> new TreeEntry.Attribute(name, attribute))));
        }
        if (info.getOperations().length > 0) {
            groups.add(group(
                    new TreeEntry.Operations(name),
                    Arrays.stream(info.getOperations()).map(operation -> new TreeEntry.Operation(name, operation))));
        }
        if (metadata.broadcaster()) {
            groups.add(group(
                    new TreeEntry.Notifications(name, TreeEntry.Notifications.label(false, 0)),
                    Arrays.stream(info.getNotifications())
                            .map(notification -> new TreeEntry.Notification(name, notification))));
        }
        item.getChildren().addAll(0, groups);
    }

    /** Changes the label of the MBean's Notifications node, if it has one. */
    public void setNotificationsLabel(ObjectName name, String label) {
        find(name).ifPresent(item -> {
            for (TreeItem<TreeEntry> child : item.getChildren()) {
                if (child.getValue() instanceof TreeEntry.Notifications) {
                    child.setValue(new TreeEntry.Notifications(name, label));
                }
            }
        });
    }

    private MBeanTreeItem folder(TreeItem<TreeEntry> parent, MBeanPath.Step step) {
        MBeanTreeItem item = items.get(step.id());
        if (item == null) {
            item = new MBeanTreeItem(new TreeEntry.Folder(step.label(), step.tooltip()));
            insertSorted(parent, item);
            items.put(step.id(), item);
        }
        return item;
    }

    private static TreeItem<TreeEntry> group(TreeEntry entry, Stream<? extends TreeEntry> children) {
        TreeItem<TreeEntry> group = new MBeanTreeItem(entry);
        group.getChildren().setAll(children.map(MBeanTreeItem::new).toList());
        return group;
    }

    private static void insertSorted(TreeItem<TreeEntry> parent, TreeItem<TreeEntry> child) {
        List<TreeItem<TreeEntry>> children = parent.getChildren();
        int index = 0;
        while (index < children.size() && ORDER.compare(children.get(index).getValue(), child.getValue()) <= 0) {
            index++;
        }
        children.add(index, child);
    }

    private static boolean isMetadata(TreeEntry entry) {
        return rank(entry) < 3;
    }

    private static int rank(TreeEntry entry) {
        return switch (entry) {
            case TreeEntry.Attributes _ -> 0;
            case TreeEntry.Operations _ -> 1;
            case TreeEntry.Notifications _ -> 2;
            default -> 3;
        };
    }
}
