/*
 * Copyright (c) 2026 JConsole Booster contributors.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

package com.druvu.jconsole.ui.core;

import com.druvu.jconsole.jmx.ProxyClient;
import com.druvu.jconsole.plugins.ExceptionSafePlugin;
import java.awt.EventQueue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the periodic update {@link Timer} and schedules a {@link SwingWorker} per {@link Tab} and per plugin on each
 * tick. Has no Swing component hierarchy — it is not a {@code JComponent}. It reaches back into its owning
 * {@link VMPanel} only for tab enablement and selected-tab bookkeeping, which must happen on the EDT.
 */
final class VMUpdateCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(VMUpdateCoordinator.class);

    private final VMPanel vmPanel;
    private final ProxyClient proxyClient;
    private final int updateInterval;
    private final Map<ExceptionSafePlugin, SwingWorker<?, ?>> plugins = new LinkedHashMap<>();
    private final Object lockObject = new Object();

    private Timer timer;
    private boolean wasConnected = false;
    private boolean everConnected = false;
    private boolean initialUpdate = true;

    VMUpdateCoordinator(VMPanel vmPanel, ProxyClient proxyClient, int updateInterval) {
        this.vmPanel = vmPanel;
        this.proxyClient = proxyClient;
        this.updateInterval = updateInterval;
    }

    int getUpdateInterval() {
        return updateInterval;
    }

    void addPlugin(ExceptionSafePlugin plugin) {
        plugins.put(plugin, null);
    }

    Set<ExceptionSafePlugin> getPlugins() {
        return plugins.keySet();
    }

    void markInitialUpdate() {
        initialUpdate = true;
    }

    boolean wasEverConnected() {
        return everConnected;
    }

    boolean wasConnectedAtLastCheck() {
        return wasConnected;
    }

    void clearWasConnected() {
        wasConnected = false;
    }

    void start() {
        if (timer != null) {
            timer.cancel();
        }
        TimerTask timerTask = new TimerTask() {
            public void run() {
                update();
            }
        };
        String timerName = "Timer-" + vmPanel.getConnectionName();
        timer = new Timer(timerName, true);
        timer.schedule(timerTask, 0, updateInterval);
    }

    void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    // Called on a TimerTask thread. Any GUI manipulation must be performed with invokeLater() or invokeAndWait().
    private void update() {
        synchronized (lockObject) {
            if (!proxyClient.isConnected()) {
                if (wasConnected) {
                    EventQueue.invokeLater(vmPanel::handleConnectionLost);
                }
                wasConnected = false;
                return;
            } else {
                wasConnected = true;
                everConnected = true;
            }
            proxyClient.flush();
            List<Tab> tabs = vmPanel.getTabs();
            final int n = tabs.size();
            for (int i = 0; i < n; i++) {
                final int index = i;
                try {
                    if (!proxyClient.isDead()) {
                        tabs.get(index).update();
                        if (initialUpdate) {
                            EventQueue.invokeLater(() -> vmPanel.setEnabledAt(index, true));
                        }
                    }
                } catch (Exception e) {
                    if (initialUpdate) {
                        EventQueue.invokeLater(() -> vmPanel.setEnabledAt(index, false));
                    }
                }
            }

            // plugin GUI update
            for (ExceptionSafePlugin p : plugins.keySet()) {
                SwingWorker<?, ?> sw = p.newSwingWorker();
                SwingWorker<?, ?> prevSW = plugins.get(p);
                // schedule SwingWorker to run only if the previous SwingWorker
                // has finished its task and it hasn't started.
                if (prevSW == null || prevSW.isDone()) {
                    if (sw == null || sw.getState() == SwingWorker.StateValue.PENDING) {
                        plugins.put(p, sw);
                        if (sw != null) {
                            p.executeSwingWorker(sw);
                        }
                    }
                }
            }

            // Set the first enabled tab in the tab's list as the selected tab on initial update.
            if (initialUpdate) {
                EventQueue.invokeLater(() -> {
                    int index = vmPanel.getSelectedIndex();
                    if (index < 0 || !vmPanel.isEnabledAt(index)) {
                        for (int i = 0; i < n; i++) {
                            if (vmPanel.isEnabledAt(i)) {
                                vmPanel.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                });
                initialUpdate = false;
            }
        }
    }
}
