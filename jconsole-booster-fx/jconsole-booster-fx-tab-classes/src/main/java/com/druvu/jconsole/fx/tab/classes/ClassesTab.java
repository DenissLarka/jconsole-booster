package com.druvu.jconsole.fx.tab.classes;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.vm.JmxDataAccess;
import com.druvu.jconsole.fx.tab.BackgroundCall;
import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.JmxRead;
import com.druvu.jconsole.fx.tab.TabHost;
import java.io.IOException;
import javafx.scene.Node;

/**
 * The Classes tab: loaded classes charted over time, the loaded, unloaded and total counts, and verbose class loading
 * on demand; a port of the Swing JConsole's Classes tab.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ClassesTab implements ConnectionTab {

    private final JmxDataAccess connection;
    private final TabHost host;

    /** Built at first use on the FX thread, the only thread that touches it. */
    private ClassesView view;

    ClassesTab(JmxDataAccess connection, TabHost host) {
        this.connection = connection;
        this.host = host;
    }

    @Override
    public String id() {
        return "classes";
    }

    @Override
    public String title() {
        return Messages.CLASSES;
    }

    @Override
    public int place() {
        return 40;
    }

    @Override
    public Node content() {
        return view().root();
    }

    @Override
    public Runnable refresh() throws IOException {
        ClassLoadingReading reading = read();
        return () -> view().show(reading);
    }

    /**
     * Turns the target JVM's report of each class it loads on or off, off the FX thread; a failure is shown through the
     * app.
     *
     * @return the thread making the change
     */
    Thread setVerbose(boolean verbose) {
        return BackgroundCall.start(
                Messages.VERBOSE_OUTPUT,
                () -> connection.getClassLoadingMXBean().setVerbose(verbose),
                host);
    }

    private ClassLoadingReading read() throws IOException {
        return JmxRead.run(() -> ClassLoadingReading.read(connection.getClassLoadingMXBean()));
    }

    private ClassesView view() {
        if (view == null) {
            view = new ClassesView(this::setVerbose);
        }
        return view;
    }
}
