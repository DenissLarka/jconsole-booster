package com.druvu.jconsole.fx.tab.threads;

import com.druvu.jconsole.fx.tab.JmxRead;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * Runs the reads the user asks for (a thread's stack, a deadlock search) away from the FX thread, since a JMX call
 * blocks, and hands each outcome back to the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class BackgroundReads {

    private final Executor background;

    /** Reads run by {@code background}; tests pass one that runs them at once. */
    BackgroundReads(Executor background) {
        this.background = background;
    }

    /** A virtual thread per read: a read mostly waits on the network, which a virtual thread does cheaply. */
    static BackgroundReads onVirtualThreads() {
        return new BackgroundReads(
                read -> Thread.ofVirtual().name("threads-tab-read").start(read));
    }

    /**
     * Runs {@code read} in the background.
     *
     * @param read the read from the target JVM
     * @param show takes the result, on the FX thread
     * @param failed takes the failure, on the FX thread
     */
    <T> void read(JmxRead<T> read, Consumer<T> show, Consumer<Exception> failed) {
        background.execute(() -> {
            try {
                T result = JmxRead.run(read);
                Platform.runLater(() -> show.accept(result));
            } catch (IOException | RuntimeException failure) {
                Platform.runLater(() -> failed.accept(failure));
            }
        });
    }
}
