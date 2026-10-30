/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.druvu.jconsole.inspector;

import com.druvu.jconsole.core.mbeans.MBeanValues;
import com.druvu.jconsole.inspector.operations.widgets.ParamWidget;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

/** The Swing half of JConsole's inspector utilities; the value types live in {@link MBeanValues}. */
public class Utils {

    private Utils() {}

    private static final Set<Integer> tableNavigationKeys = Set.of(
            KeyEvent.VK_TAB,
            KeyEvent.VK_ENTER,
            KeyEvent.VK_HOME,
            KeyEvent.VK_END,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_RIGHT,
            KeyEvent.VK_UP,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_PAGE_UP,
            KeyEvent.VK_PAGE_DOWN);

    /**
     * This method is responsible for converting the inputs given by the user into a useful object array for passing
     * into a parameter array.
     */
    public static Object[] getParameters(ParamWidget[] inputs, String[] params) throws Exception {
        Object result[] = new Object[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            Object userInput = inputs[i].getValue();
            // if it's already a complex object, use the value;
            // if the widget produced bytes (file picker for byte[]), pass through;
            // else try to instantiate with string constructor.
            if (userInput instanceof XObject) {
                result[i] = ((XObject) userInput).getObject();
            } else if (userInput instanceof byte[]) {
                result[i] = userInput;
            } else {
                result[i] = MBeanValues.createObjectFromString(params[i].toString(), (String) userInput);
            }
        }
        return result;
    }

    @SuppressWarnings("serial")
    public static class ReadOnlyTableCellEditor extends DefaultCellEditor {

        public ReadOnlyTableCellEditor(JTextField tf) {
            super(tf);
            tf.addFocusListener(new Utils.EditFocusAdapter(this));
            tf.addKeyListener(new Utils.CopyKeyAdapter());
        }
    }

    public static class EditFocusAdapter extends FocusAdapter {

        private CellEditor editor;

        public EditFocusAdapter(CellEditor editor) {
            this.editor = editor;
        }

        @Override
        public void focusLost(FocusEvent e) {
            editor.stopCellEditing();
        }
    }

    public static class CopyKeyAdapter extends KeyAdapter {
        private static final String defaultEditorKitCopyActionName = DefaultEditorKit.copyAction;
        private static final String transferHandlerCopyActionName =
                (String) TransferHandler.getCopyAction().getValue(Action.NAME);

        @Override
        public void keyPressed(KeyEvent e) {
            // Accept "copy" key strokes
            KeyStroke ks = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiersEx());
            JComponent comp = (JComponent) e.getSource();
            for (int i = 0; i < 3; i++) {
                InputMap im = comp.getInputMap(i);
                Object key = im.get(ks);
                if (defaultEditorKitCopyActionName.equals(key) || transferHandlerCopyActionName.equals(key)) {
                    return;
                }
            }
            // Accept JTable navigation key strokes
            if (!tableNavigationKeys.contains(e.getKeyCode())) {
                e.consume();
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
            e.consume();
        }
    }
}
