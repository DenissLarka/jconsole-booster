/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.druvu.jconsole.inspector.operations;

import com.druvu.jconsole.inspector.Utils;
import com.druvu.jconsole.inspector.operations.widgets.FileWidget;
import com.druvu.jconsole.inspector.operations.widgets.ParamWidget;
import com.druvu.jconsole.inspector.operations.widgets.ParamWidgetFactory;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class OperationEntry extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(OperationEntry.class);

    private MBeanOperationInfo operation;
    private ParamWidget[] inputs;
    private final OperationStateStore stateStore;
    private final String mbeanClassName;

    public OperationEntry(
            MBeanOperationInfo operation,
            boolean isCallable,
            JButton button,
            XOperations xoperations,
            OperationStateStore stateStore,
            String mbeanClassName) {
        super(new BorderLayout());
        this.operation = operation;
        this.stateStore = stateStore;
        this.mbeanClassName = mbeanClassName;
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setPanel(isCallable, button, xoperations);
    }

    private void setPanel(boolean isCallable, JButton button, XOperations xoperations) {
        try {
            MBeanParameterInfo params[] = operation.getSignature();
            inputs = new ParamWidget[params.length];
            for (int i = 0; i < params.length; i++) {
                if (params[i].getName() != null) {
                    JLabel name = new JLabel(params[i].getName(), JLabel.CENTER);
                    String hintProse = ParameterHint.parse(params[i].getDescription())
                            .map(ParameterHint::prose)
                            .orElse(params[i].getDescription());
                    name.setToolTipText(hintProse);
                    add(name);
                }

                String preset = (stateStore == null)
                        ? null
                        : stateStore.load(mbeanClassName, operation.getName(), params[i].getName());
                inputs[i] = ParamWidgetFactory.create(params[i], isCallable, button, xoperations, preset);
                attachForgetMenu(inputs[i], operation.getName(), params[i].getName());
                add(inputs[i].component());
            }
            validate();
            doLayout();
        } catch (Exception e) {
            logger.error("Error setting Operation panel", e);
        }
    }

    public String[] getSignature() {
        MBeanParameterInfo params[] = operation.getSignature();
        String result[] = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            result[i] = params[i].getType();
        }
        return result;
    }

    public Object[] getParameters() throws Exception {
        MBeanParameterInfo params[] = operation.getSignature();
        String signature[] = new String[params.length];
        for (int i = 0; i < params.length; i++) signature[i] = params[i].getType();
        return Utils.getParameters(inputs, signature);
    }

    public String getReturnType() {
        return operation.getReturnType();
    }

    public MBeanOperationInfo getOperationInfo() {
        return operation;
    }

    /**
     * Persists the current input values for next time. Skips file-picker
     * widgets (the bytes themselves aren't useful to re-fill — see plan
     * Phase&nbsp;2.B). Called by {@link XOperations} on a successful invoke.
     */
    public void saveCurrentValues() {
        if (stateStore == null || inputs == null) {
            return;
        }
        MBeanParameterInfo[] params = operation.getSignature();
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] instanceof FileWidget) {
                continue;
            }
            Object value = inputs[i].getValue();
            if (value instanceof String s) {
                stateStore.save(mbeanClassName, operation.getName(), params[i].getName(), s);
            }
        }
    }

    private void attachForgetMenu(ParamWidget widget, String opName, String paramName) {
        if (stateStore == null) {
            return;
        }
        JPopupMenu popup = new JPopupMenu();
        JMenuItem forget = new JMenuItem("Forget this value");
        forget.addActionListener(e -> stateStore.forget(mbeanClassName, opName, paramName));
        popup.add(forget);
        widget.component().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShow(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShow(e);
            }

            private void maybeShow(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
}
