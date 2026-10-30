package com.druvu.jconsole.ui.menu;

/** Visual divider between bookmarks within a group ({@code ---} in the file). */
public final class BookmarkSeparator implements BookmarkEntry {

    public static final BookmarkSeparator INSTANCE = new BookmarkSeparator();

    private BookmarkSeparator() {}
}
