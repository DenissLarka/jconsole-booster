package com.druvu.jconsole.plugins.jtop;
/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */

/*
 *
 * Example of using the java.lang.management API to sort threads
 * by CPU usage.
 *
 * JTop class can be run as a standalone application.
 * It first establishes a connection to a target VM specified
 * by the given hostname and port number where the JMX agent
 * to be connected.  It then polls for the thread information
 * and the CPU consumption of each thread to display every 2
 * seconds.
 *
 * It is also used by JTopPlugin, which is a JConsolePlugin
 * that can be used with JConsole (see README.txt). The JTop
 * GUI will be added as a JConsole tab by the JTop plugin.
 *
 * @see com.sun.tools.jconsole.JConsolePlugin
 *
 * @author Mandy Chung
 */

import static java.lang.management.ManagementFactory.THREAD_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.newPlatformMXBeanProxy;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import javax.management.MBeanServerConnection;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import lombok.extern.slf4j.Slf4j;

/** JTop is a JPanel to display a thread's name, CPU time, and its state in a table. */
@Slf4j
public class JTop extends JPanel {

    private static class StatusBar extends JPanel {
        private static final long serialVersionUID = -6483392381797633018L;
        private final JLabel statusText;

        public StatusBar(boolean defaultVisible) {
            super(new GridLayout(1, 1));
            statusText = new JLabel();
            statusText.setVisible(defaultVisible);
            add(statusText);
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension maximum = super.getMaximumSize();
            Dimension minimum = getMinimumSize();
            return new Dimension(maximum.width, minimum.height);
        }

        public void setMessage(String text) {
            statusText.setText(text);
            statusText.setVisible(true);
        }
    }

    private static final long serialVersionUID = -1499762160973870696L;
    private MBeanServerConnection server;
    private ThreadMXBean tmbean;
    private MyTableModel tmodel;
    private final StatusBar statusBar;

    public JTop() {
        super(new GridBagLayout());

        tmodel = new MyTableModel();
        JTable table = new JTable(tmodel);
        table.setPreferredScrollableViewportSize(new Dimension(500, 300));

        // Set the renderer to format Double
        table.setDefaultRenderer(Double.class, new DoubleRenderer());
        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        // Add the scroll pane to this panel.
        GridBagConstraints c1 = new GridBagConstraints();
        c1.fill = GridBagConstraints.BOTH;
        c1.gridy = 0;
        c1.gridx = 0;
        c1.weightx = 1;
        c1.weighty = 1;
        add(scrollPane, c1);

        statusBar = new StatusBar(false);
        GridBagConstraints c2 = new GridBagConstraints();
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.gridy = 1;
        c2.gridx = 0;
        c2.weightx = 1.0;
        c2.weighty = 0.0;
        add(statusBar, c2);
    }

    // Set the MBeanServerConnection object for communicating
    // with the target VM
    public void setMBeanServerConnection(MBeanServerConnection mbs) {
        this.server = mbs;
        try {
            this.tmbean = newPlatformMXBeanProxy(server, THREAD_MXBEAN_NAME, ThreadMXBean.class);
        } catch (IOException e) {
            log.error("Failed to create ThreadMXBean proxy", e);
        }
        if (!tmbean.isThreadCpuTimeSupported()) {
            statusBar.setMessage("Monitored VM does not support thread CPU time measurement");
        } else {
            try {
                tmbean.setThreadCpuTimeEnabled(true);
            } catch (SecurityException e) {
                statusBar.setMessage("Monitored VM does not have permission for enabling thread cpu time measurement");
            }
        }
    }

    class MyTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -7877310288576779514L;
        private String[] columnNames = {"ThreadName", "CPU(sec)", "State"};
        // List of all threads. The key of each entry is the CPU time,
        // and its value is the ThreadInfo object with no stack trace.
        private List<Map.Entry<Long, ThreadInfo>> threadList = Collections.emptyList();

        public MyTableModel() {}

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return threadList.size();
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            Map.Entry<Long, ThreadInfo> me = threadList.get(row);
            switch (col) {
                case 0:
                    // Column 0 shows the thread name
                    return me.getValue().getThreadName();
                case 1:
                    // Column 1 shows the CPU usage
                    long ns = me.getKey().longValue();
                    double sec = ns / 1000000000;
                    return new Double(sec);
                case 2:
                    // Column 2 shows the thread state
                    return me.getValue().getThreadState();
                default:
                    return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        void setThreadList(List<Map.Entry<Long, ThreadInfo>> list) {
            threadList = list;
        }
    }

    /** Get the thread list with CPU consumption and the ThreadInfo for each thread sorted by the CPU time. */
    private List<Map.Entry<Long, ThreadInfo>> getThreadList() {
        // Get all threads and their ThreadInfo objects
        // with no stack trace
        long[] tids = tmbean.getAllThreadIds();
        ThreadInfo[] tinfos = tmbean.getThreadInfo(tids);

        // build a map with key = CPU time and value = ThreadInfo
        SortedMap<Long, ThreadInfo> map = new TreeMap<>();
        for (int i = 0; i < tids.length; i++) {
            long cpuTime = tmbean.getThreadCpuTime(tids[i]);
            // filter out threads that have been terminated
            if (cpuTime != -1 && tinfos[i] != null) {
                map.put(new Long(cpuTime), tinfos[i]);
            }
        }

        // build the thread list and sort it with CPU time
        // in decreasing order
        Set<Map.Entry<Long, ThreadInfo>> set = map.entrySet();
        List<Map.Entry<Long, ThreadInfo>> list = new ArrayList<>(set);
        Collections.reverse(list);
        return list;
    }

    /** Format Double with 4 fraction digits */
    class DoubleRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1704639497162584382L;
        NumberFormat formatter;

        public DoubleRenderer() {
            super();
            setHorizontalAlignment(JLabel.RIGHT);
        }

        @Override
        public void setValue(Object value) {
            if (formatter == null) {
                formatter = NumberFormat.getInstance();
                formatter.setMinimumFractionDigits(4);
            }
            setText((value == null) ? "" : formatter.format(value));
        }
    }

    // SwingWorker responsible for updating the GUI
    //
    // It first gets the thread and CPU usage information as a
    // background task done by a worker thread so that
    // it will not block the event dispatcher thread.
    //
    // When the worker thread finishes, the event dispatcher
    // thread will invoke the done() method which will update
    // the UI.
    class Worker extends SwingWorker<List<Map.Entry<Long, ThreadInfo>>, Object> {
        private MyTableModel tmodel;

        Worker(MyTableModel tmodel) {
            this.tmodel = tmodel;
        }

        // Get the current thread info and CPU time
        @Override
        public List<Map.Entry<Long, ThreadInfo>> doInBackground() {
            return getThreadList();
        }

        // fire table data changed to trigger GUI update
        // when doInBackground() is finished
        @Override
        protected void done() {
            try {
                // Set table model with the new thread list
                tmodel.setThreadList(get());
                // refresh the table model
                tmodel.fireTableDataChanged();
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
            }
        }
    }

    // Return a new SwingWorker for UI update
    public SwingWorker<?, ?> newSwingWorker() {
        return new Worker(tmodel);
    }
}
