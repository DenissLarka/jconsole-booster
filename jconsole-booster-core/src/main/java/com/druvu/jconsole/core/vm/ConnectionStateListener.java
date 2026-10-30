package com.druvu.jconsole.core.vm;

/**
 * Told each time the connection to the target JVM changes state. It is called on the thread that changed the state, so
 * a user interface hands the change over to its own thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@FunctionalInterface
public interface ConnectionStateListener {

    void connectionStateChanged(ConnectionState oldState, ConnectionState newState);
}
