package com.druvu.jconsole.ui.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.core.vm.ProxyClient;
import com.sun.tools.jconsole.JConsoleContext;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;
import org.testng.annotations.Test;

/** {@link SwingConnectionContext} hands the core's connection-state changes to Swing: plugin-API states, on the EDT. */
public class SwingConnectionContextTest {

    @Test
    public void stateChangesArriveOnTheEventThreadAsPluginStates() throws Exception {
        ProxyClient client = clientForAClosedPort();
        SwingConnectionContext context = new SwingConnectionContext(client);
        List<String> events = recordEvents(context);
        try {
            client.connect(); // refused: CONNECTING, then DISCONNECTED
            SwingUtilities.invokeAndWait(() -> {}); // runs after the events queued on the EDT
        } finally {
            context.close();
        }

        assertThat(events)
                .containsExactly(
                        "connectionState DISCONNECTED->CONNECTING on EDT",
                        "connectionState CONNECTING->DISCONNECTED on EDT");
        assertThat(context.getConnectionState()).isEqualTo(JConsoleContext.ConnectionState.DISCONNECTED);
    }

    @Test
    public void aClosedContextStopsFollowingTheConnection() throws Exception {
        ProxyClient client = clientForAClosedPort();
        SwingConnectionContext context = new SwingConnectionContext(client);
        List<String> events = recordEvents(context);
        context.close();

        client.connect();
        SwingUtilities.invokeAndWait(() -> {});

        assertThat(events).isEmpty();
    }

    private static List<String> recordEvents(SwingConnectionContext context) {
        List<String> events = new CopyOnWriteArrayList<>();
        context.addPropertyChangeListener(event -> events.add(event.getPropertyName() + " " + event.getOldValue() + "->"
                + event.getNewValue() + (SwingUtilities.isEventDispatchThread() ? " on EDT" : " off EDT")));
        return events;
    }

    private static ProxyClient clientForAClosedPort() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        return ProxyClient.getProxyClient("service:jmx:jmxmp://localhost:" + port, null, null, false);
    }
}
