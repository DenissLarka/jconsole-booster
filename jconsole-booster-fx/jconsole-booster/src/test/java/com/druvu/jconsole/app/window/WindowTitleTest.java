package com.druvu.jconsole.app.window;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** Covers the window title: the connection's name while connected, "(disconnected)" otherwise. */
public class WindowTitleTest {

    @Test
    public void connectedShowsTheConnectionsName() {
        assertThat(WindowTitle.connectedTo("admin@service:jmx:jmxmp://localhost:7091"))
                .isEqualTo("JConsole Booster — admin@service:jmx:jmxmp://localhost:7091");
    }

    @Test
    public void otherwiseDisconnected() {
        assertThat(WindowTitle.notConnected()).isEqualTo("JConsole Booster (disconnected)");
    }
}
