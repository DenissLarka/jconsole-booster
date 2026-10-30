package com.druvu.jconsole.app.connect;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** Covers a connection request: the short and canonical target forms, and a text form without the password. */
public class ConnectionRequestTest {

    @Test
    public void aHostAndPortIsTheDefaultJmxmp() {
        ConnectionRequest request = ConnectionRequest.to(" localhost:7091 ");

        assertThat(request.url()).isEqualTo("service:jmx:jmxmp://localhost:7091");
        assertThat(request.shortUrl()).isEqualTo("localhost:7091");
        assertThat(request.userName()).isNull();
        assertThat(request.requireValidChain()).isFalse();
    }

    @Test
    public void anotherServiceUrlStaysAsWritten() {
        String rmi = "service:jmx:rmi:///jndi/rmi://host:9999/jmxrmi";

        assertThat(ConnectionRequest.to(rmi).shortUrl()).isEqualTo(rmi);
    }

    @Test
    public void theCanonicalFormMatchesBothWaysOfWritingATarget() {
        assertThat(ConnectionRequest.canonical("service:jmx:jmxmp://h:1")).isEqualTo("h:1");
        assertThat(ConnectionRequest.canonical("h:1")).isEqualTo("h:1");
        assertThat(ConnectionRequest.canonical(null)).isEmpty();
        assertThat(ConnectionRequest.canonical("  ")).isEmpty();
    }

    @Test
    public void theTextFormNeverShowsThePassword() {
        ConnectionRequest request = new ConnectionRequest("service:jmx:jmxmp://h:1", "admin", "s3cret", true);

        assertThat(request.toString()).contains("admin@service:jmx:jmxmp://h:1").doesNotContain("s3cret");
    }
}
