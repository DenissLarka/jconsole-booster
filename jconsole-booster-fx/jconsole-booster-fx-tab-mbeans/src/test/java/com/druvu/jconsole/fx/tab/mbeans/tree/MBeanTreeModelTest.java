package com.druvu.jconsole.fx.tab.mbeans.tree;

import static com.druvu.jconsole.fx.tab.mbeans.testing.TestServers.name;
import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.mbeans.common.MBeanMetadata;
import java.util.List;
import javafx.scene.control.TreeItem;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** The tree's nodes follow MBeans coming and going, and show an MBean's metadata nodes first. */
public class MBeanTreeModelTest {

    private static final ObjectName YOUNG = name("java.lang:type=GarbageCollector,name=Young");
    private static final ObjectName OLD = name("java.lang:type=GarbageCollector,name=Old");
    private static final ObjectName RUNTIME = name("java.lang:type=Runtime");

    private MBeanTreeModel model;

    @BeforeMethod
    public void newModel() {
        model = new MBeanTreeModel();
    }

    @Test
    public void foldersAreSharedAndSortedByLabel() {
        model.add(YOUNG);
        model.add(RUNTIME);
        model.add(OLD);

        TreeItem<TreeEntry> domain = single(model.root().getChildren());
        assertThat(labels(domain.getChildren())).containsExactly("GarbageCollector", "Runtime");
        assertThat(labels(domain.getChildren().getFirst().getChildren())).containsExactly("Old", "Young");
        assertThat(domain.getChildren().get(1).getValue()).isEqualTo(new TreeEntry.MBean(RUNTIME, "Runtime"));
    }

    @Test
    public void addingTwiceChangesNothing() {
        model.add(RUNTIME);
        model.add(RUNTIME);

        assertThat(single(model.root().getChildren()).getChildren()).hasSize(1);
    }

    @Test
    public void removingTheLastMBeanPrunesTheEmptyFolders() {
        model.add(YOUNG);
        model.add(OLD);
        model.remove(YOUNG);

        assertThat(model.find(YOUNG)).isEmpty();
        assertThat(model.find(OLD)).isPresent();

        model.remove(OLD);

        assertThat(model.root().getChildren()).isEmpty();
    }

    @Test
    public void aFolderThatIsAnMBeanTooTurnsBackIntoAFolder() {
        ObjectName parent = name("d:type=Pool");
        ObjectName child = name("d:type=Pool,name=one");
        model.add(child);
        model.add(parent);

        TreeItem<TreeEntry> pool = model.find(parent).orElseThrow();
        assertThat(pool.getValue()).isInstanceOf(TreeEntry.MBean.class);

        model.remove(parent);

        assertThat(pool.getValue()).isEqualTo(new TreeEntry.Folder("Pool", "type=Pool"));
        assertThat(model.find(child)).isPresent();
    }

    @Test
    public void metadataNodesComeFirstWithTheirChildren() {
        ObjectName parent = name("d:type=Pool");
        model.add(name("d:type=Pool,name=one"));
        TreeItem<TreeEntry> pool = model.add(parent);

        assertThat(pool.isLeaf())
                .as("an MBean offers to expand before its children are read")
                .isFalse();
        assertThat(model.requestMetadata(pool)).isTrue();
        assertThat(model.requestMetadata(pool)).as("asked once").isFalse();
        model.addMetadata(pool, new MBeanMetadata(info(), true));

        assertThat(labels(pool.getChildren())).containsExactly("Attributes", "Operations", "Notifications", "one");
        assertThat(labels(pool.getChildren().getFirst().getChildren())).containsExactly("Size");
        assertThat(pool.getChildren().get(1).getChildren().getFirst().getValue().tooltip())
                .isEqualTo("resize(int)");
    }

    @Test
    public void noNotificationsNodeForAnMBeanThatEmitsNone() {
        TreeItem<TreeEntry> runtime = model.add(RUNTIME);
        model.requestMetadata(runtime);
        model.addMetadata(runtime, new MBeanMetadata(info(), false));

        assertThat(labels(runtime.getChildren())).containsExactly("Attributes", "Operations");
    }

    @Test
    public void theNotificationsLabelFollowsTheBuffer() {
        TreeItem<TreeEntry> runtime = model.add(RUNTIME);
        model.requestMetadata(runtime);
        model.addMetadata(runtime, new MBeanMetadata(info(), true));

        model.setNotificationsLabel(RUNTIME, TreeEntry.Notifications.label(true, 4));

        assertThat(runtime.getChildren().get(2).getValue().label()).isEqualTo("Notifications[4]");
        assertThat(TreeEntry.Notifications.label(false, 4)).isEqualTo("Notifications");
    }

    private static MBeanInfo info() {
        return new MBeanInfo(
                "test.Pool",
                "A pool",
                new MBeanAttributeInfo[] {new MBeanAttributeInfo("Size", "int", "The size", true, false, false)},
                new MBeanConstructorInfo[0],
                new MBeanOperationInfo[] {
                    new MBeanOperationInfo(
                            "resize",
                            "Resizes",
                            new MBeanParameterInfo[] {new MBeanParameterInfo("size", "int", "")},
                            "void",
                            MBeanOperationInfo.ACTION)
                },
                new MBeanNotificationInfo[] {
                    new MBeanNotificationInfo(new String[] {"pool.full"}, "javax.management.Notification", "Full")
                });
    }

    private static TreeItem<TreeEntry> single(List<TreeItem<TreeEntry>> items) {
        assertThat(items).hasSize(1);
        return items.getFirst();
    }

    private static List<String> labels(List<TreeItem<TreeEntry>> items) {
        return items.stream().map(item -> item.getValue().label()).toList();
    }
}
