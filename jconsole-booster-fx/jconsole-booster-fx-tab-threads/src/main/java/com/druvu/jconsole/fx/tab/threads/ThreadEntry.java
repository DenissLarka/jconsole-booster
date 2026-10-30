package com.druvu.jconsole.fx.tab.threads;

/**
 * A thread of the target JVM as the thread lists show it.
 *
 * @param id the thread's id
 * @param name the thread's name
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record ThreadEntry(long id, String name) {

    /** A list shows a thread by its name. */
    @Override
    public String toString() {
        return name;
    }
}
