package com.druvu.jconsole.ui.menu;

import com.druvu.jconsole.launcher.ArgumentParser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a bookmarked connection URL to its display label, for surfaces that show raw URLs but read better with the
 * friendly name — currently the Connect dialog's history combo, where a list of look-alike {@code host:port} entries is
 * hard to scan.
 *
 * <p>Keys are canonicalised with {@link #canonical}, so a bookmark hand-written in {@code connections.txt} as a full
 * {@code service:jmx:} URL still matches the short {@code host:port} form the MRU records, and vice versa.
 */
public final class BookmarkLabels {

    private BookmarkLabels() {}

    /** Canonical URL → plain-text label for every bookmark in {@code connections.txt}. */
    public static Map<String, String> byUrl() {
        return byUrl(ConnectionBookmarksMenu.loadGroups());
    }

    /** Package-visible for tests: the mapping rule, independent of where the file lives. */
    static Map<String, String> byUrl(List<BookmarkGroup> groups) {
        Map<String, String> out = new LinkedHashMap<>();
        for (BookmarkGroup group : groups) {
            for (BookmarkEntry entry : group.entries()) {
                if (!(entry instanceof Bookmark bookmark)) {
                    continue;
                }
                String key = canonical(bookmark.url());
                String label = MarkupRenderer.plain(bookmark.displayName()).strip();
                if (!key.isEmpty() && !label.isEmpty()) {
                    // First bookmark wins: the same host may be listed in several groups.
                    out.putIfAbsent(key, label);
                }
            }
        }
        return out;
    }

    /**
     * The form both the MRU and the bookmark writer store, so the two match on a plain string compare: a bare
     * {@code host:port} expanded to a service URL and collapsed back, other service URLs left as written.
     */
    public static String canonical(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return ArgumentParser.shortenUrl(ArgumentParser.adaptUrl(url.strip()));
    }
}
