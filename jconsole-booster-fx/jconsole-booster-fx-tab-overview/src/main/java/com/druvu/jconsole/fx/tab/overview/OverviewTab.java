package com.druvu.jconsole.fx.tab.overview;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.JmxRead;
import java.io.IOException;
import java.util.Optional;
import javafx.scene.Node;

/**
 * The Overview tab: on every update tick it reads the target JVM's heap, threads, classes and CPU usage on the update
 * thread, and hands the view a step that only adds the values to its charts.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class OverviewTab implements ConnectionTab {

    static final String ID = "overview";
    static final int PLACE = 10;

    /** Keeps the previous CPU reading between ticks; used on the update thread only. */
    private final OverviewReader reader;

    /** Built at first use on the FX thread, the only thread that touches it. */
    private OverviewView view;

    OverviewTab(OverviewReader reader) {
        this.reader = reader;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return Messages.OVERVIEW;
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
        Optional<OverviewSample> sample = JmxRead.run(reader::read);
        if (sample.isEmpty()) {
            // a target without platform MXBeans has nothing to chart
            return () -> {};
        }
        OverviewSample reading = sample.get();
        return () -> view().show(reading);
    }

    private OverviewView view() {
        if (view == null) {
            view = new OverviewView();
        }
        return view;
    }
}
