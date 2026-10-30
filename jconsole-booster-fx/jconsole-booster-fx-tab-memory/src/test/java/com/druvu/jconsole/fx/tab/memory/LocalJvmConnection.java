package com.druvu.jconsole.fx.tab.memory;

import com.druvu.jconsole.core.vm.JmxDataAccess;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * A connection to this JVM's own memory beans, read in process, with chosen answers replaced. A call the Memory tab
 * does not make fails.
 */
final class LocalJvmConnection {

    /** A replaced answer: given the call's arguments, returns the result or throws as a JMX read may. */
    @FunctionalInterface
    interface Answer {
        Object answer(Object[] args) throws IOException, JMException;
    }

    private static final MBeanServer SERVER = ManagementFactory.getPlatformMBeanServer();

    private final Map<String, Answer> answers = new HashMap<>();

    LocalJvmConnection() {
        answer("getMemoryMXBean", args -> ManagementFactory.getMemoryMXBean());
        answer("getGarbageCollectorMXBeans", args -> ManagementFactory.getGarbageCollectorMXBeans());
        answer("getMBeans", args -> mbeans((String) args[0]));
        answer("getAttributes", args -> SERVER.getAttributes((ObjectName) args[0], (String[]) args[1]));
    }

    /** Replaces the answer of every method of that name. */
    LocalJvmConnection answer(String methodName, Answer answer) {
        answers.put(methodName, answer);
        return this;
    }

    JmxDataAccess connection() {
        return proxy(JmxDataAccess.class, (proxy, method, args) -> {
            Answer answer = answers.get(method.getName());
            if (answer == null) {
                throw new UnsupportedOperationException(method.getName());
            }
            return answer.answer(args);
        });
    }

    /**
     * A bean whose every call fails as a remote MXBean proxy's does once the connection is gone: with an IOException
     * wrapped in an UndeclaredThrowableException.
     */
    static <T> T lostBean(Class<T> type) {
        return proxy(type, (proxy, method, args) -> {
            throw new IOException("connection lost");
        });
    }

    /** {@code bean} with the method named {@code methodName} replaced by {@code answer}. */
    static <T> T replacing(Class<T> type, T bean, String methodName, Answer answer) {
        return proxy(
                type,
                (proxy, method, args) ->
                        method.getName().equals(methodName) ? answer.answer(args) : method.invoke(bean, args));
    }

    private static Map<ObjectName, MBeanInfo> mbeans(String domain) throws JMException {
        Map<ObjectName, MBeanInfo> mbeans = new HashMap<>();
        for (ObjectName name : SERVER.queryNames(new ObjectName(domain + ":*"), null)) {
            mbeans.put(name, SERVER.getMBeanInfo(name));
        }
        return mbeans;
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(
                Proxy.newProxyInstance(LocalJvmConnection.class.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
