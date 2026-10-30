package com.druvu.jconsole.fx.tab.summary;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.vm.JmxDataAccess;
import com.druvu.jconsole.fx.tab.ConnectionTab;
import java.io.IOException;
import java.util.Optional;
import javafx.scene.Node;

/**
 * The VM Summary tab: on every update tick it reads the target JVM's summary on the update thread and hands the page a
 * step that only sets texts.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class SummaryTab implements ConnectionTab {

    static final String ID = "summary";
    static final int PLACE = 50;

    private final SummaryReader reader;

    // built on first use, on the FX thread
    private SummaryView view;

    SummaryTab(JmxDataAccess connection) {
        this.reader = new SummaryReader(connection, connection.getDisplayName());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return Messages.SUMMARY_TAB_TAB_NAME;
    }

    @Override
    public int place() {
        return PLACE;
    }

    @Override
    public Node content() {
        return view().node();
    }

    @Override
    public Runnable refresh() throws IOException {
        Optional<VmSummary> summary = reader.read();
        if (summary.isPresent()) {
            VmSummary reading = summary.get();
            return () -> view().show(reading);
        }
        return () -> view().showUnavailable();
    }

    private SummaryView view() {
        if (view == null) {
            view = new SummaryView();
        }
        return view;
    }
}
