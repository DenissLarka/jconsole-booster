package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.core.mbeans.MBeanValues;
import com.druvu.jconsole.core.operations.ParameterHint;
import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.TabHost;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

/**
 * One operation's row: the operation's cell, its return type above the invoke button, and a cell per parameter, its
 * name above its input. The input is filled with the value remembered for the MBean class, or else with the
 * descriptor's default value. The return type and the names are {@link HelpLink}s. An operation whose parameters the
 * form cannot supply is shown disabled.
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
    private final Node operationCell;
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
        // the buttons fill their column, so their names line up on the left, and never shrink below their name
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setAlignment(Pos.CENTER_LEFT);
        Label returns = HelpLink.create(
                ParameterInputs.simpleName(operation.getReturnType()), operation.getName(), operationHelp(operation));
        operationCell = new VBox(2, returns, button);
        Runnable invokeOnEnter = () -> {
            if (callable) {
                invoke.accept(this);
            }
        };
        for (MBeanParameterInfo parameter : operation.getSignature()) {
            Label name = HelpLink.create(parameter.getName(), parameter.getName(), parameterHelp(parameter));
            String preset = memory.load(mbeanClass, operation.getName(), parameter.getName())
                    .orElse(ParameterDescriptor.defaultValue(parameter));
            ParameterInput input = ParameterInputs.create(parameter, preset, invokeOnEnter, host);
            input.onForget(() -> memory.forget(mbeanClass, operation.getName(), parameter.getName()));
            input.node().setDisable(!callable);
            inputs.add(input);
            parameterCells.add(new VBox(2, name, input.node()));
        }
    }

    /** A parameter's help: its description when the MBean gives one, then its full type. */
    static String parameterHelp(MBeanParameterInfo parameter) {
        return help(
                parameter.getDescription(),
                Messages.TYPE + ": " + MBeanValues.getReadableClassName(parameter.getType()));
    }

    /** An operation's help: its description when the MBean gives one, then the full type it returns. */
    static String operationHelp(MBeanOperationInfo operation) {
        return help(
                operation.getDescription(),
                Messages.RETURNS + ": " + MBeanValues.getReadableClassName(operation.getReturnType()));
    }

    /** The description without its markup, then the type line; only the type line when there is no description. */
    private static String help(String description, String typeLine) {
        String prose = description == null
                ? ""
                : ParameterHint.parse(description).map(ParameterHint::prose).orElse(description);
        return prose.isBlank() ? typeLine : prose + "\n\n" + typeLine;
    }

    MBeanOperationInfo operation() {
        return operation;
    }

    Button button() {
        return button;
    }

    /** The operation's cell: its return type above the invoke button. */
    Node operationCell() {
        return operationCell;
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
