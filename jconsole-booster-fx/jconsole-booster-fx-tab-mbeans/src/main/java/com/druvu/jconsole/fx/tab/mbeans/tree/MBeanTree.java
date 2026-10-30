package com.druvu.jconsole.fx.tab.mbeans.tree;

import com.druvu.jconsole.fx.tab.mbeans.common.MBeanMetadata;
import com.druvu.lib.fx.util.FxThreads;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MBean tree on screen: domains, key-property folders, MBeans. An MBean's Attributes, Operations and Notifications
 * nodes are read from the server when it is first expanded; a single click on an MBean expands it. Call on the FX
 * thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class MBeanTree {

    private static final Logger log = LoggerFactory.getLogger(MBeanTree.class);

    private final MBeanTreeModel model = new MBeanTreeModel();
    private final TreeView<TreeEntry> view = new TreeView<>(model.root());
    private final Function<ObjectName, CompletionStage<MBeanMetadata>> metadataReader;

    /** @param metadataReader reads an MBean's metadata off the FX thread */
    public MBeanTree(Function<ObjectName, CompletionStage<MBeanMetadata>> metadataReader) {
        this.metadataReader = metadataReader;
        view.setShowRoot(false);
        view.setCellFactory(tree -> new EntryCell());
        model.root()
                .addEventHandler(TreeItem.<TreeEntry>branchExpandedEvent(), event -> readMetadata(event.getTreeItem()));
    }

    public Node node() {
        return view;
    }

    /** Calls {@code action} with the entry the user selects, {@code null} when the selection is cleared. */
    public void onSelect(Consumer<TreeEntry> action) {
        view.getSelectionModel()
                .selectedItemProperty()
                .addListener((property, before, item) -> action.accept(item == null ? null : item.getValue()));
    }

    public void addAll(Collection<ObjectName> names) {
        names.forEach(this::add);
    }

    public void add(ObjectName name) {
        TreeItem<TreeEntry> item = model.add(name);
        // a folder that became an MBean while open shows its children right away
        if (item.isExpanded()) {
            readMetadata(item);
        }
    }

    public void remove(ObjectName name) {
        model.remove(name);
    }

    public void setNotificationsLabel(ObjectName name, String label) {
        model.setNotificationsLabel(name, label);
    }

    private void readMetadata(TreeItem<TreeEntry> item) {
        if (!(item.getValue() instanceof TreeEntry.MBean mbean) || !model.requestMetadata(item)) {
            return;
        }
        metadataReader
                .apply(mbean.name())
                .whenCompleteAsync(
                        (metadata, failure) -> {
                            if (failure != null) {
                                log.debug("Could not read the metadata of {}", mbean.name(), failure);
                                model.metadataFailed(item);
                                return;
                            }
                            model.addMetadata(item, metadata);
                        },
                        FxThreads.fxExecutor());
    }

    /** Shows the entry's label and tooltip; a single click on an MBean expands it. */
    private static final class EntryCell extends TreeCell<TreeEntry> {

        private final Tooltip tooltip = new Tooltip();

        EntryCell() {
            setOnMouseClicked(this::expandMBean);
        }

        @Override
        protected void updateItem(TreeEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setText(null);
                setTooltip(null);
                return;
            }
            setText(entry.label());
            tooltip.setText(entry.tooltip());
            setTooltip(entry.tooltip().isEmpty() ? null : tooltip);
        }

        private void expandMBean(MouseEvent event) {
            if (event.getButton() == MouseButton.PRIMARY
                    && event.getClickCount() == 1
                    && getItem() instanceof TreeEntry.MBean
                    && !onDisclosure(event)) {
                getTreeItem().setExpanded(true);
            }
        }

        /** Whether the click hit the expand arrow, which toggles by itself. */
        private boolean onDisclosure(MouseEvent event) {
            Node disclosure = getDisclosureNode();
            for (Node hit = event.getPickResult().getIntersectedNode(); hit != null; hit = hit.getParent()) {
                if (hit == disclosure) {
                    return true;
                }
            }
            return false;
        }
    }
}
