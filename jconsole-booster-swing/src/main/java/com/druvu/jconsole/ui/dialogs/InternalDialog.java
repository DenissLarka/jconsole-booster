/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static javax.swing.JLayeredPane.*;

import com.druvu.jconsole.launcher.JConsole;
import com.druvu.jconsole.util.Messages;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Used instead of JDialog in a JDesktopPane/JInternalFrame environment. */
@SuppressWarnings("serial")
public class InternalDialog extends JInternalFrame {
    protected JLabel statusBar;

    public InternalDialog(JConsole jConsole, String title, boolean modal) {
        super(title, true, true, false, false);

        setLayer(PALETTE_LAYER);
        putClientProperty("JInternalFrame.frameType", "optionDialog");

        jConsole.getDesktopPane().add(this);

        getActionMap().put("cancel", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                setVisible(false);
                if (statusBar != null) {
                    statusBar.setText("");
                }
            }
        });
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    }

    public void setLocationRelativeTo(Component c) {
        setLocation((c.getWidth() - getWidth()) / 2, (c.getHeight() - getHeight()) / 2);
    }

    protected class MastheadIcon implements Icon {
        private static final int HEIGHT = 60;
        private static final int GAP = 16;
        private Font font = Font.decode(Messages.MASTHEAD_FONT);
        private String title;

        public MastheadIcon(String title) {
            this.title = title;
        }

        public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
            g = g.create();
            int width = c.getWidth();
            int height = getIconHeight();

            g.setColor(new Color(0x35556b));
            g.fillRect(0, y, width, height);

            g.setFont(font);
            ((Graphics2D) g)
                    .setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int textHeight = g.getFontMetrics(font).getAscent();
            g.setColor(Color.white);
            g.drawString(title, GAP, y + height / 2 + textHeight / 2);
        }

        public int getIconWidth() {
            int textWidth = 0;
            Graphics g = getGraphics();
            if (g != null) {
                FontMetrics fm = g.getFontMetrics(font);
                if (fm != null) {
                    textWidth = fm.stringWidth(title);
                }
            }
            return GAP + textWidth + GAP;
        }

        public int getIconHeight() {
            return HEIGHT;
        }
    }
}
