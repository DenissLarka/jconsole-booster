package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;

/**
 * An MBean's operations, sorted by name, one row each: the return type (its full name in the tooltip), the invoke
 * button, the parameter inputs.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class OperationsView {

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
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(4);
        grid.setPadding(new Insets(6));
        List<MBeanOperationInfo> sorted = operations.stream()
                .sorted(Comparator.comparing(MBeanOperationInfo::getName))
                .toList();
        for (int row = 0; row < sorted.size(); row++) {
            MBeanOperationInfo operation = sorted.get(row);
            Label returns = new Label(ParameterInputs.simpleName(operation.getReturnType()));
            if (operation.getReturnType() != null) {
                returns.setTooltip(new Tooltip(operation.getReturnType()));
            }
            GridPane.setHalignment(returns, HPos.RIGHT);
            OperationForm form = new OperationForm(operation, mbeanClass, memory, services.host(), call::invoke);
            forms.add(form);
            grid.addRow(row, returns, form.button(), form.parameters());
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
}
