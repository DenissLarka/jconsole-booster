package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;

/**
 * An MBean's operations as a table, sorted by name: a header row, then one row per operation with the operation's cell
 * (its return type above the invoke button) and a column per parameter (its name above its input). It is a grid drawn
 * like a table rather than a TableView, because a TableView reuses its cells while scrolling, which takes the focus out
 * of a field being typed in.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class OperationsView {

    /** The space above and below each cell, so the buttons of neighbouring rows do not touch. */
    private static final Insets CELL_MARGIN = new Insets(4, 0, 4, 0);

    private static final String HEADER_STYLE = "-fx-font-weight: bold;";
    private static final String HEADER_LINE_STYLE =
            "-fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;";
    private static final String ROW_LINE_STYLE = "-fx-border-color: -color-border-muted; -fx-border-width: 0 0 1 0;";

    private final List<OperationForm> forms = new ArrayList<>();
    private final ScrollPane pane;

    /** @param mbeanClass the MBean's class, the key of the remembered parameter values */
    public OperationsView(
            TabServices services,
            OperationMemory memory,
            ObjectName name,
            String mbeanClass,
            List<MBeanOperationInfo> operations) {
        OperationCall call = new OperationCall(services, name);
        List<MBeanOperationInfo> sorted = operations.stream()
                .sorted(Comparator.comparing(MBeanOperationInfo::getName))
                .toList();
        int parameterColumns = sorted.stream()
                .mapToInt(operation -> operation.getSignature().length)
                .max()
                .orElse(0);
        int columns = 1 + parameterColumns;
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setPadding(new Insets(0, 6, 6, 6));
        addHeader(grid, columns, parameterColumns);
        for (int i = 0; i < sorted.size(); i++) {
            int row = i + 1;
            OperationForm form = new OperationForm(sorted.get(i), mbeanClass, memory, services.host(), call::invoke);
            forms.add(form);
            grid.add(line(ROW_LINE_STYLE), 0, row, columns, 1);
            addCell(grid, form.operationCell(), 0, row);
            List<Node> parameters = form.parameterCells();
            for (int p = 0; p < parameters.size(); p++) {
                addCell(grid, parameters.get(p), 1 + p, row);
            }
        }
        pane = new ScrollPane(grid);
        pane.setFitToWidth(true);
    }

    public Node node() {
        return pane;
    }

    /** The rows, for tests. */
    List<OperationForm> forms() {
        return List.copyOf(forms);
    }

    private static void addHeader(GridPane grid, int columns, int parameterColumns) {
        grid.add(line(HEADER_LINE_STYLE), 0, 0, columns, 1);
        addCell(grid, header(Messages.OPERATION), 0, 0);
        for (int p = 0; p < parameterColumns; p++) {
            addCell(grid, header(Messages.PARAMETER + " " + (p + 1)), 1 + p, 0);
        }
    }

    private static Label header(String text) {
        Label header = new Label(text);
        header.setStyle(HEADER_STYLE);
        return header;
    }

    /** The cells keep to the top of their row, so the hints line up beside a tall text area. */
    private static void addCell(GridPane grid, Node cell, int column, int row) {
        GridPane.setMargin(cell, CELL_MARGIN);
        GridPane.setValignment(cell, VPos.TOP);
        grid.add(cell, column, row);
    }

    /** The line under a row: a region across every column, added before the row's cells so it lies behind them. */
    private static Region line(String style) {
        Region line = new Region();
        line.setStyle(style);
        line.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return line;
    }
}
