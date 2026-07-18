package com.druvu.jconsole.console;

import com.druvu.jconsole.jmx.JMXConnectionManager.ConnectionResult;
import com.druvu.jconsole.jmx.ProxyClient.Snapshot;
import com.druvu.jconsole.jmx.ProxyClient.SnapshotMBeanServerConnection;
import java.io.IOException;
import javax.management.MBeanServerConnection;

/**
 * One live console connection: the connected URL, the {@link ConnectionResult}, and a
 * {@link SnapshotMBeanServerConnection} (a caching reflection proxy over the raw connection, flushed before each read
 * to see fresh values — mirrors what {@code VMPanel} uses in the GUI). Write commands use the raw
 * {@link #connection()}.
 */
final class ConsoleSession {

    private final String url;
    private final ConnectionResult result;
    private final SnapshotMBeanServerConnection snapshot;

    ConsoleSession(String url, ConnectionResult result) {
        this.url = url;
        this.result = result;
        this.snapshot = Snapshot.newSnapshot(result.connection());
    }

    String url() {
        return url;
    }

    ConnectionResult result() {
        return result;
    }

    SnapshotMBeanServerConnection snapshot() {
        return snapshot;
    }

    MBeanServerConnection connection() {
        return result.connection();
    }

    void close() {
        try {
            result.connector().close();
        } catch (IOException ignore) {
            // best-effort close on the way out
        }
    }
}
