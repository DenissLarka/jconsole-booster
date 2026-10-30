package com.druvu.jconsole.fx.tab.overview;

/**
 * The target JVM's thread counts.
 *
 * @param live the live threads
 * @param peak the most live threads at once so far
 * @param totalStarted the threads started since the JVM started
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record ThreadCounts(int live, int peak, long totalStarted) {}
