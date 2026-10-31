package com.druvu.jconsole.fx.tab.mbeans;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.mbeans.common.MBeanAccess;
import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import com.druvu.jconsole.fx.tab.mbeans.operations.OperationsView;
import com.druvu.jconsole.fx.tab.mbeans.testing.Gadget;
import com.druvu.jconsole.fx.tab.mbeans.testing.TestServers;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.JMException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The operations table: a header row, then per operation its return type, its button and a column per parameter. */
public class OperationsTableTest {

    private TabServices services;
    private GridPane grid;

    @BeforeClass
    public void buildTheTable() throws JMException {
        FxTestToolkit.ensureStarted();
        MBeanServer server = TestServers.withGadget();
        services = new TabServices(new MBeanAccess(server), new RecordingHost());
        List<MBeanOperationInfo> operations =
                List.of(server.getMBeanInfo(TestServers.GADGET).getOperations());
        grid = FxTestToolkit.call(() -> {
            OperationsView view = new OperationsView(
                    services, new MemoryOnlyOperationMemory(), TestServers.GADGET, Gadget.class.getName(), operations);
            return (GridPane) ((ScrollPane) view.node()).getContent();
        });
    }

    @AfterClass(alwaysRun = true)
    public void closeTheServices() {
        services.close();
    }

    @Test
    public void theHeaderNamesTheColumns() {
        assertThat(FxTestToolkit.call(() -> textsInRow(0))).containsExactly("Return type", "Operation", "Parameter 1");
    }

    @Test
    public void eachOperationHasARowSortedByName() {
        assertThat(FxTestToolkit.call(() ->
                        cellsIn(1, Button.class).stream().map(Button::getText).toList()))
                .containsExactly("echo", "ping", "rename", "reset");
        assertThat(FxTestToolkit.call(() -> textsInRow(rowOf("echo")))).containsExactly("String");
    }

    @Test
    public void aParameterHasAColumnWithItsNameBeforeItsInput() {
        HBox cell = FxTestToolkit.call(() -> (HBox) cellAt(2, rowOf("rename")));

        assertThat(cell.getChildren()).hasSize(2);
        assertThat(((Label) cell.getChildren().get(0)).getText()).isEqualTo("p1");
        assertThat(cell.getChildren().get(1)).isInstanceOf(TextField.class);
    }

    @Test
    public void aParameterStartsWithItsDescriptorsDefaultAmongItsLegalValues() {
        Descriptor descriptor =
                new ImmutableDescriptor(Map.of("legalValues", new String[] {"WARN", "INFO"}, "defaultValue", "INFO"));
        MBeanOperationInfo changeLevel = new MBeanOperationInfo(
                "changeLevel",
                null,
                new MBeanParameterInfo[] {new MBeanParameterInfo("level", "java.lang.String", null, descriptor)},
                "java.lang.String",
                MBeanOperationInfo.ACTION,
                null);

        ComboBox<?> level = FxTestToolkit.call(() -> {
            OperationsView view = new OperationsView(
                    services, new MemoryOnlyOperationMemory(), TestServers.GADGET, "Levels", List.of(changeLevel));
            GridPane table = (GridPane) ((ScrollPane) view.node()).getContent();
            HBox cell = (HBox) table.getChildren().stream()
                    .filter(node -> GridPane.getColumnIndex(node) == 2 && GridPane.getRowIndex(node) == 1)
                    .findFirst()
                    .orElseThrow();
            return (ComboBox<?>) cell.getChildren().get(1);
        });

        List<Object> items = List.copyOf(level.getItems());
        assertThat(items).containsExactly("WARN", "INFO");
        assertThat(level.getValue()).isEqualTo("INFO");
    }

    private List<String> textsInRow(int row) {
        return grid.getChildren().stream()
                .filter(node -> node instanceof Label && GridPane.getRowIndex(node) == row)
                .sorted(Comparator.comparing(GridPane::getColumnIndex))
                .map(node -> ((Label) node).getText())
                .toList();
    }

    private <T extends Node> List<T> cellsIn(int column, Class<T> type) {
        return grid.getChildren().stream()
                .filter(node -> type.isInstance(node) && GridPane.getColumnIndex(node) == column)
                .sorted(Comparator.comparing(GridPane::getRowIndex))
                .map(type::cast)
                .toList();
    }

    private int rowOf(String operation) {
        return cellsIn(1, Button.class).stream()
                .filter(button -> operation.equals(button.getText()))
                .map(GridPane::getRowIndex)
                .findFirst()
                .orElseThrow();
    }

    private Node cellAt(int column, int row) {
        return grid.getChildren().stream()
                .filter(node -> GridPane.getColumnIndex(node) == column && GridPane.getRowIndex(node) == row)
                .findFirst()
                .orElseThrow();
    }
}
