package com.druvu.jconsole.fx.tab.mbeans.operations;

import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Control;

/**
 * A parameter input made of one control whose value is text: a text field, a list, a date picker, a text area or a
 * check box.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ControlInput implements ParameterInput {

    private final Control control;
    private final Control menuOwner;
    private final Supplier<String> reader;

    /**
     * @param control the control on screen
     * @param menuOwner where the context menu goes: the control, or the text editor inside it
     * @param reader reads the entered text
     */
    ControlInput(Control control, Control menuOwner, Supplier<String> reader) {
        this.control = control;
        this.menuOwner = menuOwner;
        this.reader = reader;
    }

    @Override
    public Node node() {
        return control;
    }

    @Override
    public Object value() {
        return reader.get();
    }

    @Override
    public boolean isRemembered() {
        return true;
    }

    @Override
    public void onForget(Runnable forget) {
        ForgetMenu.install(menuOwner, forget);
    }
}
