package com.druvu.jconsole.core.bookmarks;

/** Visual divider between bookmarks within a group ({@code ---} in the file). */
public final class BookmarkSeparator implements BookmarkEntry {

    public static final BookmarkSeparator INSTANCE = new BookmarkSeparator();

    private BookmarkSeparator() {}
}
