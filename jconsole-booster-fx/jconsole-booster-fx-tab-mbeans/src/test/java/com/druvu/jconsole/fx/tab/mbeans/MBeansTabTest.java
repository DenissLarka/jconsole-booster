package com.druvu.jconsole.fx.tab.mbeans;

import static com.druvu.jconsole.fx.tab.mbeans.testing.TestServers.GADGET;
import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.TabContext;
import com.druvu.jconsole.fx.tab.mbeans.testing.Gadget;
import com.druvu.jconsole.fx.tab.mbeans.testing.TestServers;
import com.druvu.jconsole.fx.tab.mbeans.tree.TreeEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The tab built against an in-JVM MBean server: the tree, the attributes, an operation, the notifications. */
public class MBeansTabTest {

    private static final long TIMEOUT_SECONDS = 10;

    private MBeanServer server;
    private RecordingHost host;
    private MemoryOnlyOperationMemory memory;
    private MBeansTab tab;
    private SplitPane content;

    @BeforeClass
    public void openTheTab() {
        FxTestToolkit.ensureStarted();
        server = TestServers.withGadget();
        host = new RecordingHost();
        memory = new MemoryOnlyOperationMemory();
        tab = new MBeansTab(new TabContext(new ServerDataAccess(server), host), memory);
        content = (SplitPane) FxTestToolkit.call(tab::content);
        await("the gadget in the tree", () -> item("test", "Gadget", "first"));
    }

    @AfterClass(alwaysRun = true)
    public void closeTheTab() {
        new MBeansTabFactory().disposeComponent(tab);
    }

    @Test
    public void theTabIsNamedPlacedAndPingsTheConnection() throws IOException {
        assertThat(tab.id()).isEqualTo("mbeans");
        assertThat(tab.title()).isEqualTo("MBeans");
        assertThat(tab.place()).isEqualTo(70);
        assertThat(tab.refresh()).isNotNull();
    }

    @Test
    public void theTreeFollowsRegistrations() throws JMException {
        ObjectName second = TestServers.name("test:type=Gadget,name=second");
        server.registerMBean(new Gadget(), second);
        await("the second gadget shown", () -> item("test", "Gadget", "second"));

        server.unregisterMBean(second);
        await("the second gadget gone", () -> item("test", "Gadget", "second") == null ? second : null);
    }

    @Test
    public void expandingAnMBeanShowsItsMetadataFirst() {
        TreeItem<TreeEntry> gadget = expandedGadget();

        assertThat(FxTestToolkit.call(() -> labels(gadget.getChildren())))
                .containsExactly("Attributes", "Operations", "Notifications");
    }

    @Test
    public void theAttributeTableShowsValuesAndFailures() {
        select(metadata("Attributes"));

        TableView<?> table = await(
                "the attribute values",
                () -> find(detail(), TableView.class).stream()
                        .filter(view -> view.getItems().size() == 4)
                        .findFirst()
                        .orElse(null));

        List<String> values = FxTestToolkit.call(() -> cellTexts(table, 1));
        assertThat(values).contains("3", "long[3]", "Unavailable");
    }

    @Test
    public void invokingAnOperationTellsAndRemembersTheValue() throws JMException {
        select(metadata("Operations"));
        Button rename = await("the rename button", () -> button("rename"));
        TextField newName = FxTestToolkit.call(() -> inputBeside(rename));

        FxTestToolkit.runAndWait(() -> {
            newName.setText("Beta");
            rename.fire();
        });

        await("the notice", () -> host.notices().contains("Method successfully invoked") ? rename : null);
        assertThat(server.getAttribute(GADGET, "Name")).isEqualTo("Beta");
        // a standard MBean names its parameters p1, p2...
        assertThat(memory.load(Gadget.class.getName(), "rename", "p1")).contains("Beta");
        assertThat(host.errors()).isEmpty();
    }

    @Test
    public void subscribingCountsTheNotificationsInTheTree() throws JMException {
        TreeItem<TreeEntry> notifications = metadata("Notifications");
        select(notifications);
        Button subscribe = await("the subscribe button", () -> button("Subscribe"));

        FxTestToolkit.runAndWait(subscribe::fire);
        await("subscribed", () -> labelled(notifications, "Notifications[0]"));
        server.invoke(GADGET, "ping", new Object[] {"hello"}, new String[] {String.class.getName()});

        await("one notification counted", () -> labelled(notifications, "Notifications[1]"));
        TableView<?> buffer =
                FxTestToolkit.call(() -> find(detail(), TableView.class).getFirst());
        assertThat(FxTestToolkit.call(() -> cellTexts(buffer, 3))).containsExactly("hello");
    }

    private TreeItem<TreeEntry> expandedGadget() {
        FxTestToolkit.runAndWait(() -> {
            item("test").setExpanded(true);
            item("test", "Gadget").setExpanded(true);
            item("test", "Gadget", "first").setExpanded(true);
        });
        return await("the gadget's metadata", () -> {
            TreeItem<TreeEntry> gadget = item("test", "Gadget", "first");
            return gadget.getChildren().size() == 3 ? gadget : null;
        });
    }

    private TreeItem<TreeEntry> metadata(String label) {
        TreeItem<TreeEntry> gadget = expandedGadget();
        return FxTestToolkit.call(() -> child(gadget, label));
    }

    @SuppressWarnings("unchecked")
    private void select(TreeItem<TreeEntry> item) {
        FxTestToolkit.runAndWait(() -> ((TreeView<TreeEntry>) content.getItems().getFirst())
                .getSelectionModel()
                .select(item));
    }

    private Node detail() {
        return content.getItems().get(1);
    }

    /** The tree node under the root at the labels; {@code null} when missing. Call on the FX thread. */
    @SuppressWarnings("unchecked")
    private TreeItem<TreeEntry> item(String... labels) {
        TreeItem<TreeEntry> item = ((TreeView<TreeEntry>) content.getItems().getFirst()).getRoot();
        for (String label : labels) {
            item = item == null ? null : child(item, label);
        }
        return item;
    }

    private Button button(String text) {
        return find(detail(), Button.class).stream()
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElse(null);
    }

    /** The text input of the operation's first parameter, in the button's row of the operations grid. */
    private static TextField inputBeside(Button operation) {
        Integer row = GridPane.getRowIndex(operation);
        return operation.getParent().getChildrenUnmodifiable().stream()
                .filter(cell -> row.equals(GridPane.getRowIndex(cell)))
                .flatMap(cell -> find(cell, TextField.class).stream())
                .findFirst()
                .orElseThrow();
    }

    private static TreeItem<TreeEntry> child(TreeItem<TreeEntry> parent, String label) {
        return parent.getChildren().stream()
                .filter(child -> label.equals(child.getValue().label()))
                .findFirst()
                .orElse(null);
    }

    private static TreeItem<TreeEntry> labelled(TreeItem<TreeEntry> item, String label) {
        return label.equals(item.getValue().label()) ? item : null;
    }

    private static List<String> labels(List<TreeItem<TreeEntry>> items) {
        return items.stream().map(item -> item.getValue().label()).toList();
    }

    private static List<String> cellTexts(TableView<?> table, int column) {
        List<String> texts = new ArrayList<>();
        for (int row = 0; row < table.getItems().size(); row++) {
            texts.add(String.valueOf(table.getColumns().get(column).getCellData(row)));
        }
        return texts;
    }

    /** The nodes of a type under {@code node}, looking into split and scroll panes, which have no skin here. */
    private static <T> List<T> find(Node node, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(node, type, found);
        return found;
    }

    private static <T> void collect(Node node, Class<T> type, List<T> found) {
        if (type.isInstance(node)) {
            found.add(type.cast(node));
            return;
        }
        switch (node) {
            case SplitPane split -> split.getItems().forEach(item -> collect(item, type, found));
            case ScrollPane scroll -> collect(scroll.getContent(), type, found);
            case Parent parent -> parent.getChildrenUnmodifiable().forEach(child -> collect(child, type, found));
            case null, default -> {
                // a leaf, or nothing yet
            }
        }
    }

    /** Asks the FX thread until {@code probe} gives something, or fails the test. */
    private static <T> T await(String what, Supplier<T> probe) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            T value = FxTestToolkit.call(probe);
            if (value != null) {
                return value;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for " + what, e);
            }
        }
        throw new AssertionError("Timed out waiting for " + what);
    }
}
