package com.druvu.jconsole.app.connect;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.text.Resources;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLHandshakeException;
import javax.security.auth.login.FailedLoginException;

/**
 * Turns a failed connect into a line the user can act on. The JMX connector wraps the real reason several layers deep,
 * so the root cause decides the text.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class ConnectErrors {

    private ConnectErrors() {}

    /** The readable reason why a connect failed; a plain "Connection failed" when there is no exception to read. */
    public static String describe(Exception failure) {
        if (failure == null) {
            return Messages.CONNECTION_FAILED;
        }
        if (failure instanceof MalformedURLException) {
            return Resources.format(Messages.INVALID_URL, failure.getMessage());
        }
        if (failure instanceof IOException || failure instanceof SecurityException) {
            Throwable cause = rootCause(failure);
            if (cause instanceof UnknownHostException) {
                return Resources.format(Messages.UNKNOWN_HOST, cause.getMessage());
            }
            if (cause instanceof ConnectException
                    || cause instanceof NoRouteToHostException
                    || cause instanceof FailedLoginException
                    || cause instanceof SSLHandshakeException) {
                return Messages.CONNECTION_FAILED + ": " + cause.getMessage();
            }
        }
        return Messages.CONNECTION_FAILED + ": " + failure.getMessage();
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
