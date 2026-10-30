package com.druvu.jconsole.fx.tab.threads;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * An MXBean whose every call fails as a remote one does once the connection is gone: an IOException that the proxy
 * wraps in an UndeclaredThrowableException.
 */
final class LostConnection {

    static final String MESSAGE = "connection lost";

    private LostConnection() {}

    static <T> T bean(Class<T> type) {
        InvocationHandler handler = (proxy, method, args) -> {
            throw new IOException(MESSAGE);
        };
        return type.cast(Proxy.newProxyInstance(LostConnection.class.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
