package com.druvu.jconsole.fx.tab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import org.testng.annotations.Test;

/** A read's result passes through; a lost connection inside the proxy's wrapper comes out as the IOException. */
public class JmxReadTest {

    @Test
    public void handsTheResultOver() throws IOException {
        assertThat(JmxRead.run(() -> "read")).isEqualTo("read");
    }

    @Test
    public void unwrapsTheLostConnection() {
        IOException lost = new IOException("connection closed");

        assertThatThrownBy(() -> JmxRead.run(() -> {
                    throw new UndeclaredThrowableException(lost);
                }))
                .isSameAs(lost);
    }

    @Test
    public void keepsAnyOtherWrappedFailureWrapped() {
        UndeclaredThrowableException notAnswered = new UndeclaredThrowableException(new IllegalStateException());

        assertThatThrownBy(() -> JmxRead.run(() -> {
                    throw notAnswered;
                }))
                .isSameAs(notAnswered);
    }
}
