package com.druvu.jconsole.app.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import org.testng.annotations.Test;

/** Covers telling a lost connection from a tab's failure: an IOException anywhere in the causes is a loss. */
public class ConnectionLossTest {

    @Test
    public void anIoExceptionIsALoss() {
        assertThat(ConnectionLoss.isLoss(new IOException("closed"))).isTrue();
        assertThat(ConnectionLoss.isLoss(new UndeclaredThrowableException(new ConnectException("refused"))))
                .isTrue();
        assertThat(ConnectionLoss.isLoss(new IllegalStateException("x", new IOException("closed"))))
                .isTrue();
    }

    @Test
    public void anythingElseIsNot() {
        assertThat(ConnectionLoss.isLoss(new IllegalStateException("bug"))).isFalse();
        assertThat(ConnectionLoss.isLoss(new UndeclaredThrowableException(new Exception("no such attribute"))))
                .isFalse();
        assertThat(ConnectionLoss.isLoss(null)).isFalse();
    }
}
