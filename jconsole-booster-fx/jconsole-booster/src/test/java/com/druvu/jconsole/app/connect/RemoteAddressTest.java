package com.druvu.jconsole.app.connect;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Covers what the Connect dialog accepts: service URLs, host:port and [ipv6]:port, and what it refuses. */
public class RemoteAddressTest {

    @DataProvider
    public Object[][] connectable() {
        return new Object[][] {
            {"localhost:7091"},
            {" localhost:7091 "},
            {"10.0.0.1:0"},
            {"prod-1.example.com:65535"},
            {"[::1]:7091"},
            {"[fe80::1%en0]:9010"},
            {"service:jmx:jmxmp://localhost:7091"},
            {"service:jmx:rmi:///jndi/rmi://host:9999/jmxrmi"}
        };
    }

    @DataProvider
    public Object[][] notConnectable() {
        return new Object[][] {
            {null},
            {""},
            {"localhost"},
            {"localhost:"},
            {":7091"},
            {"localhost:port"},
            {"localhost:65536"},
            {"localhost:-1"},
            {"a:b:c"},
            {"[::1]"},
            {"[::1]:x"},
            {"service:jmx:"}
        };
    }

    @Test(dataProvider = "connectable")
    public void acceptsATarget(String text) {
        assertThat(RemoteAddress.isValid(text)).isTrue();
    }

    @Test(dataProvider = "notConnectable")
    public void refusesWhatIsNoTarget(String text) {
        assertThat(RemoteAddress.isValid(text)).isFalse();
    }
}
