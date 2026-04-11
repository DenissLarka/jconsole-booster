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

package com.druvu.jconsole.launcher;

import com.druvu.jconsole.jmx.ProxyClient;
import com.druvu.jconsole.plugins.jtop.JTopPlugin;
import com.druvu.jconsole.ui.components.OutputViewer;
import com.druvu.jconsole.ui.core.VMInternalFrame;
import com.druvu.jconsole.ui.core.VMPanel;
import com.druvu.jconsole.ui.dialogs.AboutDialog;
import com.druvu.jconsole.ui.dialogs.ConnectDialog;
import com.druvu.jconsole.ui.dialogs.CreateMBeanDialog;
import com.druvu.jconsole.util.Messages;
import com.druvu.jconsole.util.Resources;
import com.druvu.jconsole.util.Utilities;
import com.sun.tools.jconsole.JConsolePlugin;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;
import javax.security.auth.login.FailedLoginException;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class JConsole extends JFrame implements ActionListener, InternalFrameListener {

    private static final Logger logger = LoggerFactory.getLogger(JConsole.class);

    private static void applyLookAndFeel(Color color) {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception e) {
            logger.error(Resources.format(Messages.JCONSOLE_COLON_, e.getMessage()));
        }
        if (color != null) {
            UIManager.put("nimbusBlueGrey", color);
        }
    }

    private static final String title = Messages.JAVA_MONITORING___MANAGEMENT_CONSOLE;
    public static final String ROOT_URL = "service:jmx:";

    private static int updateInterval = 4000;

    private JMenuBar menuBar;
    private JMenuItem hotspotMI, connectMI, exitMI;
    private WindowMenu windowMenu;
    private JMenuItem tileMI, cascadeMI, minimizeAllMI, restoreAllMI;
    private JMenuItem userGuideMI, aboutMI;

    private JButton connectButton;
    private JDesktopPane desktop;
    private ConnectDialog connectDialog;
    private CreateMBeanDialog createDialog;

    private ArrayList<VMInternalFrame> windows = new ArrayList<>();

    private int frameLoc = 5;
    public static boolean debug;

    public JConsole(boolean hotspot) {
        super(title);

        setRootPane(new FixedJRootPane());
        Utilities.setAccessibleDescription(this, Messages.JCONSOLE_ACCESSIBLE_DESCRIPTION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // TODO: Use Actions !

        JMenu connectionMenu = new JMenu(Messages.CONNECTION);
        connectionMenu.setMnemonic(Resources.getMnemonicInt(Messages.CONNECTION));
        menuBar.add(connectionMenu);
        if (hotspot) {
            hotspotMI = new JMenuItem(Messages.HOTSPOT_MBEANS_ELLIPSIS);
            hotspotMI.setMnemonic(Resources.getMnemonicInt(Messages.HOTSPOT_MBEANS_ELLIPSIS));
            hotspotMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
            hotspotMI.addActionListener(this);
            connectionMenu.add(hotspotMI);
            connectionMenu.addSeparator();
        }

        connectMI = new JMenuItem(Messages.NEW_CONNECTION_ELLIPSIS);
        connectMI.setMnemonic(Resources.getMnemonicInt(Messages.NEW_CONNECTION_ELLIPSIS));
        connectMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        connectMI.addActionListener(this);
        connectionMenu.add(connectMI);

        connectionMenu.addSeparator();

        exitMI = new JMenuItem(Messages.EXIT);
        exitMI.setMnemonic(Resources.getMnemonicInt(Messages.EXIT));
        exitMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
        exitMI.addActionListener(this);
        connectionMenu.add(exitMI);

        JMenu helpMenu = new JMenu(Messages.HELP_MENU_TITLE);
        helpMenu.setMnemonic(Resources.getMnemonicInt(Messages.HELP_MENU_TITLE));
        menuBar.add(helpMenu);

        if (AboutDialog.isBrowseSupported()) {
            userGuideMI = new JMenuItem(Messages.HELP_MENU_USER_GUIDE_TITLE);
            userGuideMI.setMnemonic(Resources.getMnemonicInt(Messages.HELP_MENU_USER_GUIDE_TITLE));
            userGuideMI.addActionListener(this);
            helpMenu.add(userGuideMI);
            helpMenu.addSeparator();
        }
        aboutMI = new JMenuItem(Messages.HELP_MENU_ABOUT_TITLE);
        aboutMI.setMnemonic(Resources.getMnemonicInt(Messages.HELP_MENU_ABOUT_TITLE));
        aboutMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        aboutMI.addActionListener(this);
        helpMenu.add(aboutMI);
    }

    public JDesktopPane getDesktopPane() {
        return desktop;
    }

    public List<VMInternalFrame> getInternalFrames() {
        return windows;
    }

    private void createMDI() {
        // Restore title - we now show connection name on internal frames
        setTitle(title);

        Container cp = getContentPane();
        Component oldCenter = ((BorderLayout) cp.getLayout()).getLayoutComponent(BorderLayout.CENTER);

        windowMenu = new WindowMenu(Messages.WINDOW);
        windowMenu.setMnemonic(Resources.getMnemonicInt(Messages.WINDOW));
        // Add Window menu before Help menu
        menuBar.add(windowMenu, menuBar.getComponentCount() - 1);

        desktop = new JDesktopPane();
        desktop.setBackground(new Color(235, 245, 255));

        cp.add(desktop, BorderLayout.CENTER);

        if (oldCenter instanceof VMPanel) {
            addFrame((VMPanel) oldCenter);
        }
    }

    private class WindowMenu extends JMenu {
        VMInternalFrame[] windowMenuWindows = new VMInternalFrame[0];
        int separatorPosition;

        // The width value of viewR is used to truncate long menu items.
        // The rest are placeholders and are ignored for this purpose.
        Rectangle viewR = new Rectangle(0, 0, 400, 20);
        Rectangle textR = new Rectangle(0, 0, 0, 0);
        Rectangle iconR = new Rectangle(0, 0, 0, 0);

        WindowMenu(String text) {
            super(text);

            cascadeMI = new JMenuItem(Messages.CASCADE);
            cascadeMI.setMnemonic(Resources.getMnemonicInt(Messages.CASCADE));
            cascadeMI.addActionListener(JConsole.this);
            add(cascadeMI);

            tileMI = new JMenuItem(Messages.TILE);
            tileMI.setMnemonic(Resources.getMnemonicInt(Messages.TILE));
            tileMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
            tileMI.addActionListener(JConsole.this);
            add(tileMI);

            minimizeAllMI = new JMenuItem(Messages.MINIMIZE_ALL);
            minimizeAllMI.setMnemonic(Resources.getMnemonicInt(Messages.MINIMIZE_ALL));
            minimizeAllMI.addActionListener(JConsole.this);
            add(minimizeAllMI);

            restoreAllMI = new JMenuItem(Messages.RESTORE_ALL);
            restoreAllMI.setMnemonic(Resources.getMnemonicInt(Messages.RESTORE_ALL));
            restoreAllMI.addActionListener(JConsole.this);
            add(restoreAllMI);

            separatorPosition = getMenuComponentCount();
        }

        private void add(VMInternalFrame vmIF) {
            if (separatorPosition == getMenuComponentCount()) {
                addSeparator();
            }

            int index = -1;
            int position = separatorPosition + 1;
            int n = windowMenuWindows.length;

            for (int i = 0; i < n; i++) {
                if (windowMenuWindows[i] != null) {
                    // Slot is in use, try next
                    position++;
                } else {
                    // Found a free slot
                    index = i;
                    break;
                }
            }

            if (index == -1) {
                // Create a slot at the end
                VMInternalFrame[] newArray = new VMInternalFrame[n + 1];
                System.arraycopy(windowMenuWindows, 0, newArray, 0, n);
                windowMenuWindows = newArray;
                index = n;
            }

            windowMenuWindows[index] = vmIF;

            String indexString = "" + (index + 1);
            String vmName = vmIF.getVMPanel().getDisplayName();
            // Maybe truncate menu item string and end with "..."
            String text = SwingUtilities.layoutCompoundLabel(
                    this,
                    getGraphics().getFontMetrics(getFont()),
                    indexString + " " + vmName,
                    null,
                    0,
                    0,
                    0,
                    0,
                    viewR,
                    iconR,
                    textR,
                    0);
            JMenuItem mi = new JMenuItem(text);
            if (text.endsWith("...")) {
                mi.setToolTipText(vmName);
            }

            // Set mnemonic using last digit of number
            int nDigits = indexString.length();
            mi.setMnemonic(indexString.charAt(nDigits - 1));
            mi.setDisplayedMnemonicIndex(nDigits - 1);

            mi.putClientProperty("JConsole.vmIF", vmIF);
            mi.addActionListener(JConsole.this);
            vmIF.putClientProperty("JConsole.menuItem", mi);
            add(mi, position);
        }

        private void remove(VMInternalFrame vmIF) {
            for (int i = 0; i < windowMenuWindows.length; i++) {
                if (windowMenuWindows[i] == vmIF) {
                    windowMenuWindows[i] = null;
                }
            }
            JMenuItem mi = (JMenuItem) vmIF.getClientProperty("JConsole.menuItem");
            remove(mi);
            mi.putClientProperty("JConsole.vmIF", null);
            vmIF.putClientProperty("JConsole.menuItem", null);

            if (separatorPosition == getMenuComponentCount() - 1) {
                remove(getMenuComponent(getMenuComponentCount() - 1));
            }
        }
    }

    public void actionPerformed(ActionEvent ev) {
        Object src = ev.getSource();
        if (src == hotspotMI) {
            showCreateMBeanDialog();
        }

        if (src == connectButton || src == connectMI) {
            VMPanel vmPanel = null;
            JInternalFrame vmIF = desktop.getSelectedFrame();
            if (vmIF instanceof VMInternalFrame) {
                vmPanel = ((VMInternalFrame) vmIF).getVMPanel();
            }
            String url = "";
            if (vmPanel != null && vmPanel.getUrl() != null) {
                url = vmPanel.getUrl();
            }
            showConnectDialog(url, null, null, null);
        } else if (src == tileMI) {
            tileWindows();
        } else if (src == cascadeMI) {
            cascadeWindows();
        } else if (src == minimizeAllMI) {
            for (VMInternalFrame vmIF : windows) {
                try {
                    vmIF.setIcon(true);
                } catch (PropertyVetoException ex) {
                    // Ignore
                }
            }
        } else if (src == restoreAllMI) {
            for (VMInternalFrame vmIF : windows) {
                try {
                    vmIF.setIcon(false);
                } catch (PropertyVetoException ex) {
                    // Ignore
                }
            }
        } else if (src == exitMI) {
            System.exit(0);
        } else if (src == userGuideMI) {
            AboutDialog.browseUserGuide(this);
        } else if (src == aboutMI) {
            AboutDialog.showAboutDialog(this);
        } else if (src instanceof JMenuItem) {
            JMenuItem mi = (JMenuItem) src;
            VMInternalFrame vmIF = (VMInternalFrame) mi.getClientProperty("JConsole.vmIF");
            if (vmIF != null) {
                try {
                    vmIF.setIcon(false);
                    vmIF.setSelected(true);
                } catch (PropertyVetoException ex) {
                    // Ignore
                }
                vmIF.moveToFront();
            }
        }
    }

    public void tileWindows() {
        int w = -1;
        int h = -1;
        int n = 0;
        for (VMInternalFrame vmIF : windows) {
            if (!vmIF.isIcon()) {
                n++;
                if (w == -1) {
                    try {
                        vmIF.setMaximum(true);
                        w = vmIF.getWidth();
                        h = vmIF.getHeight();
                    } catch (PropertyVetoException ex) {
                        // Ignore
                    }
                }
            }
        }
        if (n > 0 && w > 0 && h > 0) {
            int rows = (int) Math.ceil(Math.sqrt(n));
            int cols = n / rows;
            if (rows * cols < n) {
                cols++;
            }
            int x = 0;
            int y = 0;
            w /= cols;
            h /= rows;
            int col = 0;
            for (VMInternalFrame vmIF : windows) {
                if (!vmIF.isIcon()) {
                    try {
                        vmIF.setMaximum(n == 1);
                    } catch (PropertyVetoException ex) {
                        // Ignore
                    }
                    if (n > 1) {
                        vmIF.setBounds(x, y, w, h);
                    }
                    if (col < cols - 1) {
                        col++;
                        x += w;
                    } else {
                        col = 0;
                        x = 0;
                        y += h;
                    }
                }
            }
        }
    }

    public void cascadeWindows() {
        int n = 0;
        int w = -1;
        int h = -1;
        for (VMInternalFrame vmIF : windows) {
            if (!vmIF.isIcon()) {
                try {
                    vmIF.setMaximum(false);
                } catch (PropertyVetoException ex) {
                    // Ignore
                }
                n++;
                vmIF.pack();
                if (w == -1) {
                    try {
                        w = vmIF.getWidth();
                        h = vmIF.getHeight();
                        vmIF.setMaximum(true);
                        w = vmIF.getWidth() - w;
                        h = vmIF.getHeight() - h;
                        vmIF.pack();
                    } catch (PropertyVetoException ex) {
                        // Ignore
                    }
                }
            }
        }
        int x = 0;
        int y = 0;
        int dX = (n > 1) ? (w / (n - 1)) : 0;
        int dY = (n > 1) ? (h / (n - 1)) : 0;
        for (VMInternalFrame vmIF : windows) {
            if (!vmIF.isIcon()) {
                vmIF.setLocation(x, y);
                vmIF.moveToFront();
                x += dX;
                y += dY;
            }
        }
    }

    // Call on EDT
    public void addUrl(final String url, final String userName, final String password, final boolean tile) {
        new Thread("JConsole.addUrl") {
            public void run() {
                try {
                    addProxyClient(ProxyClient.getProxyClient(url, userName, password), tile);
                } catch (final MalformedURLException ex) {
                    failed(ex, url, userName, password);
                } catch (final SecurityException ex) {
                    failed(ex, url, userName, password);
                } catch (final IOException ex) {
                    failed(ex, url, userName, password);
                }
            }
        }.start();
    }

    // Call on worker thread
    void addProxyClient(final ProxyClient proxyClient, final boolean tile) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                VMPanel vmPanel = new VMPanel(proxyClient, updateInterval);
                addFrame(vmPanel);

                if (tile) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            tileWindows();
                        }
                    });
                }
                vmPanel.connect();
            }
        });
    }

    // Call on worker thread
    private void failed(final Exception ex, final String url, final String userName, final String password) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                dbgStackTrace(ex);
                showConnectDialog(url, userName, password, errorMessage(ex));
            }
        });
    }

    private VMInternalFrame addFrame(VMPanel vmPanel) {
        final VMInternalFrame vmIF = new VMInternalFrame(vmPanel);

        for (VMInternalFrame f : windows) {
            try {
                f.setMaximum(false);
            } catch (PropertyVetoException ex) {
                // Ignore
            }
        }
        desktop.add(vmIF);

        vmIF.setLocation(frameLoc, frameLoc);
        frameLoc += 30;
        vmIF.setVisible(true);
        windows.add(vmIF);
        if (windows.size() == 1) {
            try {
                vmIF.setMaximum(true);
            } catch (PropertyVetoException ex) {
                // Ignore
            }
        }
        vmIF.addInternalFrameListener(this);
        windowMenu.add(vmIF);

        return vmIF;
    }

    private void showConnectDialog(String url, String userName, String password, String msg) {
        if (connectDialog == null) {
            connectDialog = new ConnectDialog(this);
        }
        connectDialog.setConnectionParameters(url, userName, password, msg);

        connectDialog.refresh();
        connectDialog.setVisible(true);
        try {
            // Bring to front of other dialogs
            connectDialog.setSelected(true);
        } catch (PropertyVetoException e) {
        }
    }

    private void showCreateMBeanDialog() {
        if (createDialog == null) {
            createDialog = new CreateMBeanDialog(this);
        }
        createDialog.setVisible(true);
        try {
            // Bring to front of other dialogs
            createDialog.setSelected(true);
        } catch (PropertyVetoException e) {
        }
    }

    private void removeVMInternalFrame(VMInternalFrame vmIF) {
        windowMenu.remove(vmIF);
        desktop.remove(vmIF);
        desktop.repaint();
        vmIF.getVMPanel().cleanUp();
        vmIF.dispose();
    }

    private boolean isProxyClientUsed(ProxyClient client) {
        for (VMInternalFrame frame : windows) {
            ProxyClient cli = frame.getVMPanel().getProxyClient(false);
            if (client == cli) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidRemoteString(String txt) {
        boolean valid = false;
        if (txt != null) {
            txt = txt.trim();
            if (txt.startsWith(ROOT_URL)) {
                if (txt.length() > ROOT_URL.length()) {
                    valid = true;
                }
            } else {
                // ---------------------------------------
                // Supported host and port combinations:
                // hostname:port
                // IPv4Address:port
                // [IPv6Address]:port
                // ---------------------------------------

                // Is literal IPv6 address?
                //
                if (txt.startsWith("[")) {
                    int index = txt.indexOf("]:");
                    if (index != -1) {
                        // Extract literal IPv6 address
                        //
                        String address = txt.substring(1, index);
                        // Extract port
                        //
                        try {
                            String portStr = txt.substring(index + 2);
                            int port = Integer.parseInt(portStr);
                            if (port >= 0 && port <= 0xFFFF) {
                                valid = true;
                            }
                        } catch (NumberFormatException ex) {
                            valid = false;
                        }
                    }
                } else {
                    String[] s = txt.split(":");
                    if (s.length == 2) {
                        try {
                            int port = Integer.parseInt(s[1]);
                            if (port >= 0 && port <= 0xFFFF) {
                                valid = true;
                            }
                        } catch (NumberFormatException ex) {
                            valid = false;
                        }
                    }
                }
            }
        }
        return valid;
    }

    private String errorMessage(Exception ex) {
        String msg = Messages.CONNECTION_FAILED;
        if (ex instanceof IOException || ex instanceof SecurityException) {
            Throwable cause = null;
            Throwable c = ex.getCause();
            while (c != null) {
                cause = c;
                c = c.getCause();
            }
            if (cause instanceof ConnectException) {
                return msg + ": " + cause.getMessage();
            } else if (cause instanceof UnknownHostException) {
                return Resources.format(Messages.UNKNOWN_HOST, cause.getMessage());
            } else if (cause instanceof NoRouteToHostException) {
                return msg + ": " + cause.getMessage();
            } else if (cause instanceof FailedLoginException) {
                return msg + ": " + cause.getMessage();
            } else if (cause instanceof SSLHandshakeException) {
                return msg + ": " + cause.getMessage();
            }
        } else if (ex instanceof MalformedURLException) {
            return Resources.format(Messages.INVALID_URL, ex.getMessage());
        }
        return msg + ": " + ex.getMessage();
    }

    // InternalFrameListener interface

    public void internalFrameClosing(InternalFrameEvent e) {
        VMInternalFrame vmIF = (VMInternalFrame) e.getInternalFrame();
        removeVMInternalFrame(vmIF);
        windows.remove(vmIF);
        ProxyClient client = vmIF.getVMPanel().getProxyClient(false);
        if (!isProxyClientUsed(client)) {
            client.markAsDead();
        }
        if (windows.size() == 0) {
            showConnectDialog("", null, null, null);
        }
    }

    public void internalFrameOpened(InternalFrameEvent e) {}

    public void internalFrameClosed(InternalFrameEvent e) {}

    public void internalFrameIconified(InternalFrameEvent e) {}

    public void internalFrameDeiconified(InternalFrameEvent e) {}

    public void internalFrameActivated(InternalFrameEvent e) {}

    public void internalFrameDeactivated(InternalFrameEvent e) {}

    private static void mainInit(final JConsoleOptions options) {
        // Always create Swing GUI on the Event Dispatching Thread
        SwingUtilities.invokeLater(() -> {
            JConsole jConsole = new JConsole(options.hotspot());

            // Center the window on screen, taking into account screen size and insets.
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            GraphicsConfiguration gc = jConsole.getGraphicsConfiguration();
            Dimension scrSize = toolkit.getScreenSize();
            Insets scrInsets = toolkit.getScreenInsets(gc);
            scrSize.width -= 200;
            scrSize.height -= 200;
            Rectangle scrBounds = new Rectangle(
                    scrInsets.left + 100,
                    scrInsets.top + 100,
                    scrSize.width - scrInsets.left - scrInsets.right,
                    scrSize.height - scrInsets.top - scrInsets.bottom);
            jConsole.setBounds(scrBounds.x, scrBounds.y, scrBounds.width, scrBounds.height);
            jConsole.setVisible(true);
            jConsole.createMDI();

            List<String> urls = options.urls();
            boolean noTile = options.noTile();

            for (int i = 0; i < urls.size(); i++) {
                jConsole.addUrl(urls.get(i), null, null, (i == urls.size() - 1) ? !noTile : false);
            }

            if (urls.isEmpty()) {
                jConsole.showConnectDialog(null, null, null, null);
            }
        });
    }

    public static void main(String[] args) {
        if (System.getProperty("jconsole.showOutputViewer") != null) {
            OutputViewer.init();
        }

        ArgumentParser.parse(args).ifPresent(options -> {
            debug = options.debug();
            updateInterval = options.updateInterval();

            applyLookAndFeel(options.color());
            mainInit(options);
        });
    }

    public static boolean isDebug() {
        return debug;
    }

    private static void dbgStackTrace(Exception ex) {
        if (debug) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns a fresh list of bundled {@link JConsolePlugin} instances. To register an additional plugin, drop the
     * source under {@code com.druvu.jconsole.plugins.<name>} and add a {@code new XxxPlugin()} entry below.
     */
    public static synchronized List<JConsolePlugin> getPlugins() {
        return List.of(new JTopPlugin());
    }

    private static class FixedJRootPane extends JRootPane {
        /** The revalidate method seems to be the only one that gets called whenever there is a change of theme. */
        @Override
        public void revalidate() {
            // Workaround for Swing bug where the titledborder uses
            // calculated colors instead of the highlight/shadow colors
            // from the theme.
            //
            // Putting null removes any previous override and causes a
            // fallback to the current L&F's value.
            UIManager.put("TitledBorder.border", null);
            Border border = UIManager.getBorder("TitledBorder.border");
            if (border instanceof BorderUIResource.EtchedBorderUIResource) {
                Color highlight = UIManager.getColor("ToolBar.highlight");
                Color shadow = UIManager.getColor("ToolBar.shadow");
                border = new BorderUIResource.EtchedBorderUIResource(highlight, shadow);
                UIManager.put("TitledBorder.border", border);
            }
            super.revalidate();
        }
    }
}
