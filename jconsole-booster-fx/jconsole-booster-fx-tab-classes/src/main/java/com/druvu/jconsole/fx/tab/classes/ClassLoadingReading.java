package com.druvu.jconsole.fx.tab.classes;

import java.lang.management.ClassLoadingMXBean;

/**
 * One reading of the target JVM's class loading, ready for the screen.
 *
 * @param timeMillis when it was read, in epoch milliseconds
 * @param loaded the classes loaded now
 * @param totalLoaded the classes loaded since the JVM started
 * @param unloaded the classes unloaded since the JVM started
 * @param verbose whether the JVM reports each class it loads
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record ClassLoadingReading(long timeMillis, long loaded, long totalLoaded, long unloaded, boolean verbose) {

    /**
     * Reads the counts from the target's class loading bean. Every call is a JMX round trip, so this runs on the update
     * thread, never on the FX thread.
     */
    static ClassLoadingReading read(ClassLoadingMXBean classes) {
        return new ClassLoadingReading(
                System.currentTimeMillis(),
                classes.getLoadedClassCount(),
                classes.getTotalLoadedClassCount(),
                classes.getUnloadedClassCount(),
                classes.isVerbose());
    }
}
