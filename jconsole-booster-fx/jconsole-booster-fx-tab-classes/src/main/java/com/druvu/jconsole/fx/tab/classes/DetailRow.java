package com.druvu.jconsole.fx.tab.classes;

/**
 * One line of the details under the chart: a name and its value.
 *
 * @param name what the value is, without a colon
 * @param value the value in words
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record DetailRow(String name, String value) {}
