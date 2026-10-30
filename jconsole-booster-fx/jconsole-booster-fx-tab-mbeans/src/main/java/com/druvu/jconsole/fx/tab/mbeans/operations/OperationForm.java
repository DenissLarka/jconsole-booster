package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.fx.tab.TabHost;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

/**
 * One operation's row: the invoke button and an input per parameter, filled with the values remembered for the MBean
 * class. An operation whose parameters the form cannot supply is shown disabled.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class OperationForm {

    private final MBeanOperationInfo operation;
    private final String mbeanClass;
    private final OperationMemory memory;
    private final List<ParameterInput> inputs = new ArrayList<>();
    private final Button button;
    private final HBox parameters = new HBox(6);

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
        parameters.setAlignment(Pos.CENTER_LEFT);
        Runnable invokeOnEnter = () -> {
            if (callable) {
                invoke.accept(this);
            }
        };
        for (MBeanParameterInfo parameter : operation.getSignature()) {
            Label label = new Label(parameter.getName());
            String tooltip = ParameterKind.tooltip(parameter);
            if (!tooltip.isEmpty()) {
                label.setTooltip(new Tooltip(tooltip));
            }
            String preset = memory.load(mbeanClass, operation.getName(), parameter.getName())
                    .orElse("");
            ParameterInput input = ParameterInputs.create(parameter, preset, invokeOnEnter, host);
            input.onForget(() -> memory.forget(mbeanClass, operation.getName(), parameter.getName()));
            input.node().setDisable(!callable);
            inputs.add(input);
            parameters.getChildren().addAll(label, input.node());
        }
    }

    MBeanOperationInfo operation() {
        return operation;
    }

    Button button() {
        return button;
    }

    Node parameters() {
        return parameters;
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
