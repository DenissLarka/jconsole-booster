package com.druvu.jconsole.fx.charts;

/**
 * A clock tick on the time axis.
 *
 * @param time the tick's time, in epoch milliseconds
 * @param midnight whether the tick falls on local midnight, where the date shows under the clock time
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record TimeTick(long time, boolean midnight) {}
