package com.druvu.jconsole.fx.tab.jtop;

import java.io.IOException;
import java.lang.management.ThreadMXBean;

/**
 * Where the JTop tab gets the target JVM's thread MXBean: the connection in the app, this JVM's own bean in tests.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@FunctionalInterface
interface ThreadBeanSource {

    /**
     * The target JVM's thread MXBean.
     *
     * @return the bean, or null when the target JVM has none
     * @throws IOException the connection is gone
     */
    ThreadMXBean threadBean() throws IOException;
}
