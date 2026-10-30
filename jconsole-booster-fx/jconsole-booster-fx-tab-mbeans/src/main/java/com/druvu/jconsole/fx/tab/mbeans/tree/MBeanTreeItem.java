package com.druvu.jconsole.fx.tab.mbeans.tree;

import javafx.scene.control.TreeItem;

/**
 * A node of the MBean tree. An MBean node shows an expand arrow before its children are known: they are read from the
 * server only when the user first expands it.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class MBeanTreeItem extends TreeItem<TreeEntry> {

    /** Whether the Attributes, Operations and Notifications children were asked for. */
    private boolean metadataRequested;

    MBeanTreeItem(TreeEntry entry) {
        super(entry);
    }

    @Override
    public boolean isLeaf() {
        if (getValue() instanceof TreeEntry.MBean && !metadataRequested) {
            return false;
        }
        return getChildren().isEmpty();
    }

    boolean isMetadataRequested() {
        return metadataRequested;
    }

    void setMetadataRequested(boolean requested) {
        this.metadataRequested = requested;
    }
}
