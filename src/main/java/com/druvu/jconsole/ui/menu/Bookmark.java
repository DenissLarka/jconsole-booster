package com.druvu.jconsole.ui.menu;

/**
 * A single connection bookmark: a display name (possibly with inline markup
 * like {@code *bold*} or {@code [red text]}) and a JMX URL or shorthand
 * (e.g. {@code host:port}, {@code rmi:host:port}, or a full
 * {@code service:jmx:...} URL).
 */
public record Bookmark(String displayName, String url) implements BookmarkEntry {}
