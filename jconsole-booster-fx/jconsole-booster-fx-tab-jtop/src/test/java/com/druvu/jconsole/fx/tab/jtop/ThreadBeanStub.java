package com.druvu.jconsole.fx.tab.jtop;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * This JVM's own thread MXBean with chosen answers replaced, for the cases a healthy local JVM never shows. Like a
 * remote MXBean proxy, it hands over an answer's IOException wrapped in an UndeclaredThrowableException.
 */
final class ThreadBeanStub {

    /** A replaced answer: given the call's arguments, returns the result or throws. */
    @FunctionalInterface
    interface Answer {
        Object answer(Object[] args) throws IOException;
    }

    private final Map<String, Answer> answers = new HashMap<>();

    /** Replaces the answer of every method of that name. */
    ThreadBeanStub answer(String methodName, Answer answer) {
        answers.put(methodName, answer);
        return this;
    }

    ThreadMXBean bean() {
        ThreadMXBean local = ManagementFactory.getThreadMXBean();
        InvocationHandler handler = (proxy, method, args) -> {
            Answer answer = answers.get(method.getName());
            return answer == null ? method.invoke(local, args) : answer.answer(args);
        };
        return (ThreadMXBean) Proxy.newProxyInstance(
                ThreadBeanStub.class.getClassLoader(), new Class<?>[] {ThreadMXBean.class}, handler);
    }
}
