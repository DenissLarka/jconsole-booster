package com.druvu.jconsole.app.session;

import java.io.IOException;

/**
 * Tells a lost connection from a tab's own failure. A lost connection shows up as an {@link IOException}, either thrown
 * directly or wrapped by an MXBean proxy into an undeclared exception; anything else is the tab's problem.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ConnectionLoss {

    private ConnectionLoss() {}

    /** Whether {@code failure}, or any of its causes, is an {@link IOException}. */
    static boolean isLoss(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException) {
                return true;
            }
        }
        return false;
    }
}
