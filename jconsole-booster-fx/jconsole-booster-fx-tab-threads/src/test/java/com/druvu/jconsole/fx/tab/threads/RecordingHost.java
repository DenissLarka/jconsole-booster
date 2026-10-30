package com.druvu.jconsole.fx.tab.threads;

import com.druvu.jconsole.fx.tab.TabHost;
import java.util.ArrayList;
import java.util.List;
import javafx.stage.Window;

/** A host without a window that keeps the titles of the errors a tab reports. Use it on the FX thread. */
final class RecordingHost implements TabHost {

    private final List<String> errorTitles = new ArrayList<>();

    @Override
    public Window window() {
        return null;
    }

    @Override
    public void showError(String title, Throwable failure) {
        errorTitles.add(title);
    }

    @Override
    public void notice(String message) {
        // no notice is checked
    }

    List<String> errorTitles() {
        return List.copyOf(errorTitles);
    }
}
