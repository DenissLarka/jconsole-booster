package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.core.mbeans.MBeanValues;
import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.TabHost;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

/**
 * One operation's row: the invoke button and a cell per parameter, its input filled with the value remembered for the
 * MBean class, or else with the descriptor's default value. An operation whose parameters the form cannot supply is
 * shown disabled.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class OperationForm {

    /** The parameter's name sits above its input as a hint: smaller and quieter than the value. */
    private static final String NAME_STYLE = "-fx-font-size: 0.85em; -fx-text-fill: -color-fg-muted;";

    private final MBeanOperationInfo operation;
    private final String mbeanClass;
    private final OperationMemory memory;
    private final List<ParameterInput> inputs = new ArrayList<>();
    private final Button button;
    private final List<Node> parameterCells = new ArrayList<>();

    /**
     * @param mbeanClass the MBean's class, the key of the remembered values
     * @param invoke invokes the operation with this form's values
     */
    OperationForm(
            MBeanOperationInfo operation,
            String mbeanClass,
            OperationMemory memory,
            TabHost host,
            Consumer<OperationForm> invoke) {
        this.operation = operation;
        this.mbeanClass = mbeanClass;
        this.memory = memory;
        boolean callable = ParameterValues.isCallable(operation);
        button = new Button(operation.getName());
        button.setDisable(!callable);
        button.setOnAction(event -> invoke.accept(this));
        Runnable invokeOnEnter = () -> {
            if (callable) {
                invoke.accept(this);
            }
        };
        for (MBeanParameterInfo parameter : operation.getSignature()) {
            Label name = new Label(parameter.getName());
            name.setStyle(NAME_STYLE);
            name.setTooltip(new Tooltip(nameTooltip(parameter)));
            String preset = memory.load(mbeanClass, operation.getName(), parameter.getName())
                    .orElse(ParameterDescriptor.defaultValue(parameter));
            ParameterInput input = ParameterInputs.create(parameter, preset, invokeOnEnter, host);
            input.onForget(() -> memory.forget(mbeanClass, operation.getName(), parameter.getName()));
            input.node().setDisable(!callable);
            inputs.add(input);
            parameterCells.add(new VBox(2, name, input.node()));
        }
    }

    /** The parameter's description when the MBean gives one, then its full type. */
    static String nameTooltip(MBeanParameterInfo parameter) {
        String type = Messages.TYPE + ": " + MBeanValues.getReadableClassName(parameter.getType());
        String description = ParameterKind.tooltip(parameter);
        return description.isBlank() ? type : description + "\n" + type;
    }

    MBeanOperationInfo operation() {
        return operation;
    }

    Button button() {
        return button;
    }

    /** One cell per parameter, in signature order: the parameter's name above its input. */
    List<Node> parameterCells() {
        return List.copyOf(parameterCells);
    }

    /** The parameter types, as the invocation names them. */
    String[] signature() {
        return Arrays.stream(operation.getSignature())
                .map(MBeanParameterInfo::getType)
                .toArray(String[]::new);
    }

    /** What the user entered, one value per parameter. */
    Object[] entered() {
        return inputs.stream().map(ParameterInput::value).toArray();
    }

    /** Keeps the entered values for next time, after an invocation that worked; blank values are not kept. */
    void remember() {
        MBeanParameterInfo[] signature = operation.getSignature();
        for (int i = 0; i < inputs.size(); i++) {
            ParameterInput input = inputs.get(i);
            if (input.isRemembered() && input.value() instanceof String text && !text.isBlank()) {
                memory.save(mbeanClass, operation.getName(), signature[i].getName(), text);
            }
        }
    }
}
