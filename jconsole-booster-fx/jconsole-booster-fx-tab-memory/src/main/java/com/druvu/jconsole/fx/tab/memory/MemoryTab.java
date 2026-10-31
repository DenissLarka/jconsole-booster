package com.druvu.jconsole.fx.tab.memory;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.vm.JmxDataAccess;
import com.druvu.jconsole.fx.tab.BackgroundCall;
import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.JmxRead;
import com.druvu.jconsole.fx.tab.TabHost;
import java.io.IOException;
import javafx.scene.Node;

/**
 * The Memory tab: heap, non-heap and each memory pool charted over time, the pools' usage bars, the collectors' times,
 * and a garbage collection on demand; a port of the Swing JConsole's Memory tab.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class MemoryTab implements ConnectionTab {

    private final JmxDataAccess connection;
    private final TabHost host;

    /** Used on the update thread only. */
    private final MemoryReader reader;

    /** Built at first use on the FX thread, the only thread that touches it. */
    private MemoryView view;

    MemoryTab(JmxDataAccess connection, TabHost host) {
        this.connection = connection;
        this.host = host;
        this.reader = new MemoryReader(connection);
    }

    @Override
    public String id() {
        return "memory";
    }

    @Override
    public String title() {
        return Messages.MEMORY;
    }

    @Override
    public int place() {
        return 20;
    }

    @Override
    public Node content() {
        return view().root();
    }

    @Override
    public Runnable refresh() throws IOException {
        MemoryReading reading = read();
        return () -> view().show(reading);
    }

    /**
     * Asks the target JVM to collect garbage, off the FX thread; a failure is shown through the app.
     *
     * @return the thread making the request
     */
    Thread performGc() {
        return BackgroundCall.start(
                Messages.PERFORM_GC, () -> connection.getMemoryMXBean().gc(), host);
    }

    private MemoryReading read() throws IOException {
        return JmxRead.run(reader::read);
    }

    private MemoryView view() {
        if (view == null) {
            view = new MemoryView(this::performGc);
        }
        return view;
    }
}
