package com.druvu.jconsole.fx.tab.mbeans;

import com.druvu.jconsole.fx.tab.TabHost;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.stage.Window;

/**
 * An app for the tests: no window, and every error and notice written down instead of shown.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class RecordingHost implements TabHost {

    private final List<String> errors = new CopyOnWriteArrayList<>();
    private final List<String> notices = new CopyOnWriteArrayList<>();

    @Override
    public Window window() {
        return null;
    }

    @Override
    public void showError(String title, Throwable failure) {
        errors.add(title + ": " + failure);
    }

    @Override
    public void notice(String message) {
        notices.add(message);
    }

    List<String> errors() {
        return List.copyOf(errors);
    }

    List<String> notices() {
        return List.copyOf(notices);
    }
}
