package com.druvu.jconsole.fx.tab;

/**
 * One line of a tab's details: a name and its value.
 *
 * @param name what the value is, without a colon; empty on a line that continues the one above
 * @param value the value in words
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record DetailRow(String name, String value) {}
