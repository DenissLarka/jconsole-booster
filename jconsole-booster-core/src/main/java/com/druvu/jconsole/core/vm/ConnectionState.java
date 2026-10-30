package com.druvu.jconsole.core.vm;

/**
 * The state of the connection to the target JVM.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public enum ConnectionState {
    /** The connection is open. */
    CONNECTED,
    /** No connection: never opened, closed, or the last attempt failed. */
    DISCONNECTED,
    /** An attempt to connect is in progress. */
    CONNECTING
}
