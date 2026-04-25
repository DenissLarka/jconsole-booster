package com.druvu.jconsole.ui.menu;

/**
 * One row inside a {@link BookmarkGroup}: either a {@link Bookmark} (clickable connection target) or a
 * {@link BookmarkSeparator}.
 */
public sealed interface BookmarkEntry permits Bookmark, BookmarkSeparator {}
