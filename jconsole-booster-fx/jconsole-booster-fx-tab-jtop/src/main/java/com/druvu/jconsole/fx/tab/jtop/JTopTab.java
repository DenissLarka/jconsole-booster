package com.druvu.jconsole.fx.tab.jtop;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import javafx.scene.Node;

/**
 * The JTop tab: the target JVM's threads ordered by the CPU time they use, a port of the JDK's JTop demo plugin.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class JTopTab implements ConnectionTab {

    private final ThreadBeanSource source;

    /** Built at first use on the FX thread, the only thread that touches it. */
    private JTopView view;

    JTopTab(ThreadBeanSource source) {
        this.source = source;
    }

    @Override
    public String id() {
        return "jtop";
    }

    @Override
    public String title() {
        return "JTop";
    }

    @Override
    public int place() {
        return 60;
    }

    @Override
    public Node content() {
        return view().root();
    }

    @Override
    public Runnable refresh() throws IOException {
        ThreadSnapshot snapshot = read();
        return () -> view().show(snapshot);
    }

    private ThreadSnapshot read() throws IOException {
        try {
            return ThreadCpuReader.read(source.threadBean());
        } catch (UndeclaredThrowableException wrapped) {
            // An MXBean proxy hands over a lost connection as an IOException in this wrapper.
            if (wrapped.getCause() instanceof IOException lost) {
                throw lost;
            }
            throw wrapped;
        }
    }

    private JTopView view() {
        if (view == null) {
            view = new JTopView();
        }
        return view;
    }
}
