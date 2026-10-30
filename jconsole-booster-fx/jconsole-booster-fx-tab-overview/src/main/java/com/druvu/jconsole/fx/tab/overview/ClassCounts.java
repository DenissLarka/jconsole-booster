package com.druvu.jconsole.fx.tab.overview;

/**
 * The target JVM's class counts.
 *
 * @param loaded the classes loaded now
 * @param totalLoaded the classes loaded since the JVM started
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record ClassCounts(int loaded, long totalLoaded) {

    /** The classes unloaded since the JVM started: every class loaded so far, less those loaded now. */
    long unloaded() {
        return totalLoaded - loaded;
    }
}
