package com.druvu.jconsole.fx.tab.memory;

import com.druvu.jconsole.fx.tab.TabHost;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.stage.Window;

/** An app without a window that keeps the errors a tab shows, as {@code title: message}. */
final class RecordingHost implements TabHost {

    private final List<String> errors = new CopyOnWriteArrayList<>();

    @Override
    public Window window() {
        return null;
    }

    @Override
    public void showError(String title, Throwable failure) {
        errors.add(title + ": " + failure.getMessage());
    }

    @Override
    public void notice(String message) {
        // the Memory tab gives no notices
    }

    List<String> errors() {
        return List.copyOf(errors);
    }
}
