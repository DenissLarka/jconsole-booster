package com.druvu.jconsole.fx.tab.memory;

/**
 * How often a garbage collector of the target JVM has run, and for how long in all.
 *
 * @param name the collector's name
 * @param count the collections since the JVM started
 * @param timeMillis the time spent collecting since the JVM started, in milliseconds
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record CollectorTime(String name, long count, long timeMillis) {}
