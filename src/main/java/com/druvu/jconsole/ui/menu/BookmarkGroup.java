package com.druvu.jconsole.ui.menu;

import java.util.List;

/** A submenu in the bookmarks menu: a name and an ordered list of entries. */
public record BookmarkGroup(String name, List<BookmarkEntry> entries) {}
