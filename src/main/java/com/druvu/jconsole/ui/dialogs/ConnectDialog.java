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

package com.druvu.jconsole.ui.dialogs;

import static java.awt.BorderLayout.*;

import com.druvu.jconsole.launcher.ArgumentParser;
import com.druvu.jconsole.launcher.JConsole;
import com.druvu.jconsole.ui.components.LabeledComponent;
import com.druvu.jconsole.util.Messages;
import com.druvu.jconsole.util.Resources;
import com.druvu.jconsole.util.Utilities;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@SuppressWarnings("serial")
public class ConnectDialog extends InternalDialog implements DocumentListener, FocusListener {

    JConsole jConsole;
    JTextField userNameTF, passwordTF;
    JLabel remoteMessageLabel;
    JTextField remoteTF;
    JButton connectButton, cancelButton;

    private Icon mastheadIcon = new MastheadIcon(Messages.CONNECT_DIALOG_MASTHEAD_TITLE);
    private Color hintTextColor;

    private Action connectAction, cancelAction;

    public ConnectDialog(JConsole jConsole) {
        super(jConsole, Messages.CONNECT_DIALOG_TITLE, true);

        this.jConsole = jConsole;
        Utilities.setAccessibleDescription(this, Messages.CONNECT_DIALOG_ACCESSIBLE_DESCRIPTION);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        Container cp = (JComponent) getContentPane();

        JPanel formPanel = new JPanel(new java.awt.BorderLayout(0, 12));
        formPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel bottomPanel = new JPanel(new java.awt.BorderLayout());

        statusBar = new JLabel(" ", JLabel.CENTER);
        Utilities.setAccessibleName(statusBar, Messages.CONNECT_DIALOG_STATUS_BAR_ACCESSIBLE_NAME);

        Font normalLabelFont = statusBar.getFont();
        Font boldLabelFont = normalLabelFont.deriveFont(Font.BOLD);
        Font smallLabelFont = normalLabelFont.deriveFont(normalLabelFont.getSize2D() - 1);

        JLabel mastheadLabel = new JLabel(mastheadIcon);
        Utilities.setAccessibleName(mastheadLabel, Messages.CONNECT_DIALOG_MASTHEAD_ACCESSIBLE_NAME);

        cp.add(mastheadLabel, NORTH);
        cp.add(formPanel, CENTER);
        cp.add(bottomPanel, SOUTH);

        createActions();

        remoteTF = new JTextField(24);
        remoteTF.addActionListener(connectAction);
        remoteTF.getDocument().addDocumentListener(this);
        remoteTF.addFocusListener(this);
        remoteTF.setPreferredSize(remoteTF.getPreferredSize());
        Utilities.setAccessibleName(remoteTF, Messages.REMOTE_PROCESS_TEXT_FIELD_ACCESSIBLE_NAME);

        JLabel remoteLabel = new JLabel(Messages.REMOTE_PROCESS_COLON);
        remoteLabel.setFont(boldLabelFont);

        JPanel remoteTFPanel = new JPanel(new java.awt.BorderLayout());
        remoteTFPanel.add(remoteLabel, NORTH);
        remoteTFPanel.add(remoteTF, CENTER);

        remoteMessageLabel = new JLabel("<html>" + Messages.REMOTE_TF_USAGE + "</html>");
        remoteMessageLabel.setFont(smallLabelFont);
        remoteMessageLabel.setForeground(hintTextColor);
        remoteTFPanel.add(remoteMessageLabel, SOUTH);

        formPanel.add(remoteTFPanel, NORTH);

        JPanel userPwdPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        userPwdPanel.setBorder(new EmptyBorder(12, 0, 0, 0)); // top padding

        int tfWidth = 8;

        userNameTF = new JTextField(tfWidth);
        userNameTF.addActionListener(connectAction);
        userNameTF.getDocument().addDocumentListener(this);
        userNameTF.addFocusListener(this);
        Utilities.setAccessibleName(userNameTF, Messages.USERNAME_ACCESSIBLE_NAME);
        LabeledComponent lc;
        lc = new LabeledComponent(
                Messages.USERNAME_COLON_, Resources.getMnemonicInt(Messages.USERNAME_COLON_), userNameTF);
        lc.label.setFont(boldLabelFont);
        userPwdPanel.add(lc);

        passwordTF = new JPasswordField(tfWidth);
        // Heights differ, so fix here
        passwordTF.setPreferredSize(userNameTF.getPreferredSize());
        passwordTF.addActionListener(connectAction);
        passwordTF.getDocument().addDocumentListener(this);
        passwordTF.addFocusListener(this);
        Utilities.setAccessibleName(passwordTF, Messages.PASSWORD_ACCESSIBLE_NAME);

        lc = new LabeledComponent(
                Messages.PASSWORD_COLON_, Resources.getMnemonicInt(Messages.PASSWORD_COLON_), passwordTF);
        lc.setBorder(new EmptyBorder(0, 12, 0, 0)); // Left padding
        lc.label.setFont(boldLabelFont);
        userPwdPanel.add(lc);

        formPanel.add(userPwdPanel, CENTER);

        String connectButtonToolTipText = Messages.CONNECT_DIALOG_CONNECT_BUTTON_TOOLTIP;
        connectButton = new JButton(connectAction);
        connectButton.setToolTipText(connectButtonToolTipText);

        cancelButton = new JButton(cancelAction);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonPanel.setBorder(new EmptyBorder(12, 12, 2, 12));
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        bottomPanel.add(buttonPanel, NORTH);

        bottomPanel.add(statusBar, SOUTH);

        updateButtonStates();
        Utilities.updateTransparency(this);
    }

    public void revalidate() {
        // Adjust some colors
        Color disabledForeground = UIManager.getColor("Label.disabledForeground");
        if (disabledForeground == null) {
            // fall back for Nimbus that doesn't support 'Label.disabledForeground'
            disabledForeground = UIManager.getColor("Label.disabledText");
        }
        hintTextColor = Utilities.ensureContrast(disabledForeground, UIManager.getColor("Panel.background"));

        if (remoteMessageLabel != null) {
            remoteMessageLabel.setForeground(hintTextColor);
            // Update html color setting
            String colorStr = String.format("%06x", hintTextColor.getRGB() & 0xFFFFFF);
            remoteMessageLabel.setText("<html><font color=#" + colorStr + ">" + Messages.REMOTE_TF_USAGE);
        }

        super.revalidate();
    }

    private void createActions() {
        connectAction = new AbstractAction(Messages.CONNECT) {
            /* init */ {
                putValue(Action.MNEMONIC_KEY, Resources.getMnemonicInt(Messages.CONNECT));
            }

            public void actionPerformed(ActionEvent ev) {
                if (!isEnabled() || !isVisible()) {
                    return;
                }
                setVisible(false);
                statusBar.setText("");

                String txt = remoteTF.getText().trim();
                String userName = userNameTF.getText().trim();
                userName = userName.isEmpty() ? null : userName;
                String password = passwordTF.getText();
                password = password.isEmpty() ? null : password;
                try {
                    String url = ArgumentParser.adaptUrl(txt);
                    jConsole.addUrl(url, userName, password, false);
                    remoteTF.setText("");
                    userNameTF.setText("");
                    passwordTF.setText("");
                    return;
                } catch (Exception ex) {
                    statusBar.setText(ex.toString());
                }
                setVisible(true);
            }
        };

        cancelAction = new AbstractAction(Messages.CANCEL) {
            public void actionPerformed(ActionEvent ev) {
                setVisible(false);
                statusBar.setText("");
            }
        };
    }

    public void setConnectionParameters(String url, String userName, String password, String msg) {
        if (url != null && url.length() > 0) {
            remoteTF.setText(url);
            userNameTF.setText((userName != null) ? userName : "");
            passwordTF.setText((password != null) ? password : "");

            statusBar.setText((msg != null) ? msg : "");
            if (getPreferredSize().width > getWidth()) {
                pack();
            }
            remoteTF.requestFocus();
            remoteTF.selectAll();
        }
    }

    private void updateButtonStates() {
        connectAction.setEnabled(JConsole.isValidRemoteString(remoteTF.getText()));
    }

    public void insertUpdate(DocumentEvent e) {
        updateButtonStates();
    }

    public void removeUpdate(DocumentEvent e) {
        updateButtonStates();
    }

    public void changedUpdate(DocumentEvent e) {
        updateButtonStates();
    }

    public void focusGained(FocusEvent e) {
        Object source = e.getSource();
        java.awt.Component opposite = e.getOppositeComponent();

        if (!e.isTemporary()
                && source instanceof JTextField
                && opposite instanceof JComponent
                && SwingUtilities.getRootPane(opposite) == getRootPane()) {

            ((JTextField) source).selectAll();
        }

        updateButtonStates();
    }

    public void focusLost(FocusEvent e) {}

    public void setVisible(boolean b) {
        boolean wasVisible = isVisible();
        super.setVisible(b);
        if (b && !wasVisible) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    remoteTF.requestFocus();
                    remoteTF.selectAll();
                }
            });
        }
    }

    // No-op: retained so callers in JConsole that invoke refresh() after showing the
    // dialog still compile. There is no local-VM table to refresh anymore.
    public void refresh() {
        pack();
        setLocationRelativeTo(jConsole);
    }
}
