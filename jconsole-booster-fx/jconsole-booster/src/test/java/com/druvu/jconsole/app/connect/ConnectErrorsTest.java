package com.druvu.jconsole.app.connect;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLHandshakeException;
import javax.security.auth.login.FailedLoginException;
import org.testng.annotations.Test;

/** Covers the reason shown for a failed connect: the root cause decides, a bad URL and no exception at all included. */
public class ConnectErrorsTest {

    @Test
    public void aRefusedConnectionNamesTheRootCause() {
        IOException failure = new IOException("connect failed", new ConnectException("Connection refused"));

        assertThat(ConnectErrors.describe(failure)).isEqualTo("Connection failed: Connection refused");
    }

    @Test
    public void anUnknownHostIsNamed() {
        IOException failure = new IOException("x", new IOException("y", new UnknownHostException("nowhere.example")));

        assertThat(ConnectErrors.describe(failure)).isEqualTo("Unknown Host: nowhere.example");
    }

    @Test
    public void aFailedLoginAndAFailedHandshakeShowTheirOwnMessage() {
        assertThat(ConnectErrors.describe(new SecurityException("x", new FailedLoginException("Invalid credentials"))))
                .isEqualTo("Connection failed: Invalid credentials");
        assertThat(ConnectErrors.describe(new IOException("x", new SSLHandshakeException("certificate refused"))))
                .isEqualTo("Connection failed: certificate refused");
    }

    @Test
    public void aMalformedUrlIsAnInvalidUrl() {
        assertThat(ConnectErrors.describe(new MalformedURLException("Service URL must start with service:jmx:")))
                .isEqualTo("Invalid URL: Service URL must start with service:jmx:");
    }

    @Test
    public void anyOtherFailureShowsItsOwnMessage() {
        assertThat(ConnectErrors.describe(new IOException("the server closed the connection")))
                .isEqualTo("Connection failed: the server closed the connection");
        assertThat(ConnectErrors.describe(null)).isEqualTo("Connection failed");
    }
}
