package com.druvu.jconsole.fx.tab;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import javafx.application.Platform;

/**
 * Runs a JMX call the user asked for on a virtual thread, since JMX calls block and the FX thread must not, and reports
 * a failure through the app.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class BackgroundCall {

    /** A JMX call that changes the target JVM. */
    @FunctionalInterface
    public interface JmxCall {
        void run() throws IOException;
    }

    private BackgroundCall() {}

    /**
     * Starts {@code call} on a virtual thread.
     *
     * @param title the title of the error the app shows when the call fails
     * @return the thread making the call, which a caller may wait for
     */
    public static Thread start(String title, JmxCall call, TabHost host) {
        return Thread.ofVirtual().name(title).start(() -> {
            try {
                call.run();
            } catch (IOException | RuntimeException failed) {
                Throwable reason = unwrapped(failed);
                Platform.runLater(() -> host.showError(title, reason));
            }
        });
    }

    /** An MXBean proxy hands over a lost connection wrapped; the user is better told the cause. */
    private static Throwable unwrapped(Exception failed) {
        if (failed instanceof UndeclaredThrowableException wrapped && wrapped.getCause() != null) {
            return wrapped.getCause();
        }
        return failed;
    }
}
