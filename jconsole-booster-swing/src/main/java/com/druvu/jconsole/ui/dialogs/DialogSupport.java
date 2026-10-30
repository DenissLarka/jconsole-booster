package com.druvu.jconsole.ui.dialogs;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;

/**
 * Shared helpers for the security dialogs (certificate trust, plaintext-credential refusal): EDT marshaling with a
 * fail-closed default, the active window used as a dialog parent, and HTML escaping of server-supplied strings that get
 * rendered into Swing HTML labels. Kept in one place so a fix to the (subtle) EDT/interrupt handling or the escaping
 * rules applies to every dialog at once.
 */
final class DialogSupport {

    private DialogSupport() {}

    /**
     * Runs {@code work} on the EDT and returns its result. If called off the EDT, blocks until it completes; any
     * interruption or dispatch failure returns {@code failClosed} (dialogs use this to default to the safe choice).
     */
    static <T> T onEdt(Supplier<T> work, T failClosed) {
        if (SwingUtilities.isEventDispatchThread()) {
            return work.get();
        }
        AtomicReference<T> result = new AtomicReference<>(failClosed);
        try {
            SwingUtilities.invokeAndWait(() -> result.set(work.get()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failClosed;
        } catch (InvocationTargetException e) {
            return failClosed;
        }
        return result.get();
    }

    /** Runs {@code work} on the EDT without waiting (fire-and-forget), for purely informational dialogs. */
    static void onEdtAsync(Runnable work) {
        if (SwingUtilities.isEventDispatchThread()) {
            work.run();
        } else {
            SwingUtilities.invokeLater(work);
        }
    }

    static Window activeWindow() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    }

    static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
