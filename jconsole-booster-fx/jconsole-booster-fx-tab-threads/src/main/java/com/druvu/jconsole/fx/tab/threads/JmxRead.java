package com.druvu.jconsole.fx.tab.threads;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * A read from the target JVM, which may find the connection gone.
 *
 * @param <T> what the read returns
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@FunctionalInterface
interface JmxRead<T> {

    T read() throws IOException;

    /**
     * Runs {@code read}. An MXBean proxy reports a lost connection as an {@link UndeclaredThrowableException} around an
     * {@link IOException}; this hands over the IOException itself, as the tab contract expects.
     */
    static <T> T run(JmxRead<T> read) throws IOException {
        try {
            return read.read();
        } catch (UndeclaredThrowableException wrapped) {
            if (wrapped.getCause() instanceof IOException lost) {
                throw lost;
            }
            throw wrapped;
        }
    }
}
