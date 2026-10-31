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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.JMException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The operations table: a header row, then per operation its cell (the return type above the button) and a column per
 * parameter (the name above the input).
 */
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
        assertThat(FxTestToolkit.call(() -> textsInRow(0))).containsExactly("Operation", "Parameter 1");
    }

    @Test
    public void eachOperationHasARowSortedByName() {
        assertThat(FxTestToolkit.call(() -> operationCells().stream()
                        .map(cell -> button(cell).getText())
                        .toList()))
                .containsExactly("echo", "ping", "rename", "reset");
    }

    @Test
    public void theReturnTypeIsAboveTheButton() {
        VBox cell = FxTestToolkit.call(() -> cellAt(0, rowOf("echo")));

        assertThat(cell.getChildren()).hasSize(2);
        assertThat(((Label) cell.getChildren().get(0)).getText()).isEqualTo("String");
        assertThat(cell.getChildren().get(1)).isInstanceOf(Button.class);
    }

    @Test
    public void aParameterHasAColumnWithItsNameAboveItsInput() {
        VBox cell = FxTestToolkit.call(() -> cellAt(1, rowOf("rename")));

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
            VBox cell = (VBox) table.getChildren().stream()
                    .filter(node -> GridPane.getColumnIndex(node) == 1 && GridPane.getRowIndex(node) == 1)
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

    /** The operations' cells, in row order. */
    private List<VBox> operationCells() {
        return grid.getChildren().stream()
                .filter(node -> node instanceof VBox && GridPane.getColumnIndex(node) == 0)
                .sorted(Comparator.comparing(GridPane::getRowIndex))
                .map(VBox.class::cast)
                .toList();
    }

    private static Button button(VBox operationCell) {
        return (Button) operationCell.getChildren().get(1);
    }

    private int rowOf(String operation) {
        return operationCells().stream()
                .filter(cell -> operation.equals(button(cell).getText()))
                .map(GridPane::getRowIndex)
                .findFirst()
                .orElseThrow();
    }

    /** The cell at a column and row; the row's line, which starts at column 0 too, is not a cell. */
    private VBox cellAt(int column, int row) {
        return grid.getChildren().stream()
                .filter(node -> node instanceof VBox
                        && GridPane.getColumnIndex(node) == column
                        && GridPane.getRowIndex(node) == row)
                .map(VBox.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
