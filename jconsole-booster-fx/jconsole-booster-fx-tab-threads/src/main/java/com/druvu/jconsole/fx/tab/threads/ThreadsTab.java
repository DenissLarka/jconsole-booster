package com.druvu.jconsole.fx.tab.threads;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.JmxRead;
import com.druvu.jconsole.fx.tab.TabContext;
import java.io.IOException;
import java.lang.management.ThreadMXBean;
import javafx.scene.Node;

/**
 * The Threads tab: on every update tick it reads the thread counts and the live threads, and hands the view a step that
 * charts the counts and updates the list. The reads the user asks for (a thread's stack, a deadlock search) run through
 * {@link BackgroundReads}.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ThreadsTab implements ConnectionTab {

    static final String ID = "threads";
    static final int PLACE = 30;

    private final TabContext context;
    private final BackgroundReads reads;

    /** Keeps the thread names between ticks; used on the update thread only. */
    private final ThreadListReader reader = new ThreadListReader();

    /** Built at first use on the FX thread, the only thread that touches it. */
    private ThreadsView view;

    ThreadsTab(TabContext context, BackgroundReads reads) {
        this.context = context;
        this.reads = reads;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return Messages.THREADS;
    }

    @Override
    public int place() {
        return PLACE;
    }

    @Override
    public Node content() {
        return view().root();
    }

    @Override
    public Runnable refresh() throws IOException {
        ThreadMXBean threads = JmxRead.run(context.connection()::getThreadMXBean);
        if (threads == null) {
            // a target without platform MXBeans has no threads to show
            return () -> {};
        }
        ThreadListing listing = JmxRead.run(() -> reader.read(threads));
        return () -> view().show(listing);
    }

    private ThreadsView view() {
        if (view == null) {
            view = new ThreadsView(context, reads);
        }
        return view;
    }
}
