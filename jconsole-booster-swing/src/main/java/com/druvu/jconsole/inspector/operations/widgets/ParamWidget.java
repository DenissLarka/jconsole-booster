package com.druvu.jconsole.inspector.operations.widgets;

import javax.swing.JComponent;

/**
 * One operation parameter input. Concrete implementations wrap a single Swing widget (text field, combo box, checkbox,
 * file picker, …) and expose a uniform {@link #getValue()} contract that
 * {@link com.druvu.jconsole.inspector.Utils#getParameters} can consume.
 *
 * <p>{@link #getValue()} returns either a {@link String} (which is then run through
 * {@code Utils.createObjectFromString}) or — for {@code byte[]} parameters backed by a file picker — the bytes
 * directly.
 */
public interface ParamWidget {

    /** The Swing component to add to the operation row. */
    JComponent component();

    /** User-entered value. Either a {@link String} or, for the file widget, a {@code byte[]}. */
    Object getValue();
}
