/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.druvu.jconsole.inspector.ThreadDialog;
import com.druvu.jconsole.inspector.Utils;
import com.druvu.jconsole.inspector.XMBean;
import com.druvu.jconsole.launcher.JConsole;
import com.druvu.jconsole.ui.tabs.MBeansTab;
import com.druvu.jconsole.util.Messages;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial") // JDK implementation class
public abstract class XOperations extends JPanel implements ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(XOperations.class);

    public static final String OPERATION_INVOCATION_EVENT = "jam.xoperations.invoke.result";
    private java.util.List<NotificationListener> notificationListenersList;
    private Hashtable<JButton, OperationEntry> operationEntryTable;
    private XMBean mbean;
    private MBeanInfo mbeanInfo;
    private MBeansTab mbeansTab;
    private final OperationStateStore stateStore = new OperationStateStore();

    public XOperations(MBeansTab mbeansTab) {
        super(new GridLayout(1, 1));
        this.mbeansTab = mbeansTab;
        operationEntryTable = new Hashtable<JButton, OperationEntry>();
        ArrayList<NotificationListener> l = new ArrayList<NotificationListener>(1);
        notificationListenersList = Collections.synchronizedList(l);
    }

    // Call on EDT
    public void removeOperations() {
        removeAll();
    }

    // Call on EDT
    public void loadOperations(XMBean mbean, MBeanInfo mbeanInfo) {
        this.mbean = mbean;
        this.mbeanInfo = mbeanInfo;
        // add operations information
        MBeanOperationInfo operations[] = mbeanInfo.getOperations();
        Arrays.sort(operations, new Comparator<MBeanOperationInfo>() {
            @Override
            public int compare(MBeanOperationInfo o1, MBeanOperationInfo o2) {
                final String name1 = o1.getName();
                final String name2 = o2.getName();
                return name1.compareTo(name2);
            }
        });
        invalidate();

        // remove listeners, if any
        Component listeners[] = getComponents();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] instanceof JButton) {
                ((JButton) listeners[i]).removeActionListener(this);
            }
        }

        removeAll();
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        Font compactFont = new Font(Font.DIALOG, Font.PLAIN, 11);

        for (int i = 0; i < operations.length; i++) {
            // Column 0: return type (right-aligned, simple name, FQN in tooltip)
            String returnType = operations[i].getReturnType();
            JLabel methodLabel;
            if (returnType == null) {
                methodLabel = new JLabel("null", JLabel.RIGHT);
                if (JConsole.isDebug()) {
                    logger.error("WARNING: The operation's return type " + "shouldn't be \"null\". Check how the "
                            + "MBeanOperationInfo for the \"" + operations[i].getName() + "\" operation has "
                            + "been defined in the MBean's implementation code.");
                }
            } else {
                methodLabel = new JLabel(simpleReturnType(returnType), JLabel.RIGHT);
                methodLabel.setFont(compactFont);
                methodLabel.setToolTipText(returnType);
            }
            c.gridx = 0;
            c.gridy = i;
            c.gridwidth = 1;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            add(methodLabel, c);

            // Column 1: method button
            JButton methodButton = new JButton(operations[i].getName());
            methodButton.setFont(compactFont);
            boolean callable = isCallable(operations[i].getSignature());
            if (callable) {
                methodButton.addActionListener(this);
            } else {
                methodButton.setEnabled(false);
            }
            MBeanParameterInfo[] signature = operations[i].getSignature();
            OperationEntry paramEntry = new OperationEntry(
                    operations[i], callable, methodButton, this, stateStore, mbeanInfo.getClassName());
            operationEntryTable.put(methodButton, paramEntry);

            c.gridx = 1;
            c.anchor = GridBagConstraints.WEST;
            add(methodButton, c);

            // Column 2: parameters (no-arg ops leave the cell empty)
            if (signature.length > 0) {
                c.gridx = 2;
                c.weightx = 1.0;
                c.fill = GridBagConstraints.HORIZONTAL;
                add(paramEntry, c);
            }
        }

        // Glue: takes remaining vertical space so rows hug the top instead of stretching apart.
        c.gridx = 0;
        c.gridy = operations.length;
        c.gridwidth = 3;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        add(new JPanel(), c);

        validate();
    }

    /** {@code java.lang.String} → {@code String}, {@code java.lang.String[]} → {@code String[]}. */
    private static String simpleReturnType(String fqcn) {
        String readable = Utils.getReadableClassName(fqcn);
        int dot = readable.lastIndexOf('.');
        return dot < 0 ? readable : readable.substring(dot + 1);
    }

    private boolean isCallable(MBeanParameterInfo[] signature) {
        for (int i = 0; i < signature.length; i++) {
            if (!Utils.isEditableType(signature[i].getType())) {
                return false;
            }
        }
        return true;
    }

    // Call on EDT
    public void actionPerformed(final ActionEvent e) {
        performInvokeRequest((JButton) e.getSource());
    }

    public void performInvokeRequest(final JButton button) {
        final OperationEntry entryIf = operationEntryTable.get(button);
        new SwingWorker<Object, Void>() {
            @Override
            public Object doInBackground() throws Exception {
                return mbean.invoke(button.getText(), entryIf.getParameters(), entryIf.getSignature());
            }

            @Override
            protected void done() {
                try {
                    Object result = get();
                    // Successful invoke: persist the values the user just entered
                    // so the next visit pre-fills them (Phase 2.B).
                    entryIf.saveCurrentValues();
                    // sends result notification to upper level if
                    // there is a return value
                    if (entryIf.getReturnType() != null
                            && !entryIf.getReturnType().equals(Void.TYPE.getName())
                            && !entryIf.getReturnType().equals(Void.class.getName())) {
                        fireChangedNotification(OPERATION_INVOCATION_EVENT, button, result);
                    } else {
                        new ThreadDialog(
                                        button,
                                        Messages.METHOD_SUCCESSFULLY_INVOKED,
                                        Messages.INFO,
                                        JOptionPane.INFORMATION_MESSAGE)
                                .run();
                    }
                } catch (Throwable t) {
                    t = Utils.getActualException(t);
                    if (JConsole.isDebug()) {
                        t.printStackTrace();
                    }
                    new ThreadDialog(
                                    button,
                                    Messages.PROBLEM_INVOKING + " " + button.getText() + " : " + t.toString(),
                                    Messages.ERROR,
                                    JOptionPane.ERROR_MESSAGE)
                            .run();
                }
            }
        }.execute();
    }

    public MBeanOperationInfo getOperationInfo(JButton button) {
        OperationEntry entry = operationEntryTable.get(button);
        return entry == null ? null : entry.getOperationInfo();
    }

    public void addOperationsListener(NotificationListener nl) {
        notificationListenersList.add(nl);
    }

    public void removeOperationsListener(NotificationListener nl) {
        notificationListenersList.remove(nl);
    }

    // Call on EDT
    private void fireChangedNotification(String type, Object source, Object handback) {
        Notification n = new Notification(type, source, 0);
        for (NotificationListener nl : notificationListenersList) {
            nl.handleNotification(n, handback);
        }
    }

    protected abstract MBeanOperationInfo[] updateOperations(MBeanOperationInfo[] operations);
}
