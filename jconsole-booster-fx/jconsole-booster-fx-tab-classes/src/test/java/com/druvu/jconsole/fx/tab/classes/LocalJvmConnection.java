package com.druvu.jconsole.fx.tab.classes;

import com.druvu.jconsole.core.vm.JmxDataAccess;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * A connection to this JVM's own class loading bean, or to the bean given. A call the Classes tab does not make fails.
 */
final class LocalJvmConnection {

    private LocalJvmConnection() {}

    /** A connection to this JVM's class loading bean. */
    static JmxDataAccess local() {
        return to(ManagementFactory.getClassLoadingMXBean());
    }

    /** A connection whose class loading bean is {@code classes}. */
    static JmxDataAccess to(ClassLoadingMXBean classes) {
        return proxy(JmxDataAccess.class, (proxy, method, args) -> {
            if (method.getName().equals("getClassLoadingMXBean")) {
                return classes;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    /**
     * A class loading bean whose every call fails as a remote MXBean proxy's does once the connection is gone: with an
     * IOException wrapped in an UndeclaredThrowableException.
     */
    static ClassLoadingMXBean lostBean() {
        return proxy(ClassLoadingMXBean.class, (proxy, method, args) -> {
            throw new IOException("connection lost");
        });
    }

    /** This JVM's class loading bean, except that {@code setVerbose} only hands its argument to {@code verbose}. */
    static ClassLoadingMXBean keepingVerbose(VerboseSink verbose) {
        ClassLoadingMXBean local = ManagementFactory.getClassLoadingMXBean();
        return proxy(ClassLoadingMXBean.class, (proxy, method, args) -> {
            if (method.getName().equals("setVerbose")) {
                verbose.accept((Boolean) args[0]);
                return null;
            }
            return method.invoke(local, args);
        });
    }

    /** Where a stand-in bean puts the verbose state it was given, instead of turning on this JVM's class log. */
    @FunctionalInterface
    interface VerboseSink {
        void accept(boolean verbose);
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(
                Proxy.newProxyInstance(LocalJvmConnection.class.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
