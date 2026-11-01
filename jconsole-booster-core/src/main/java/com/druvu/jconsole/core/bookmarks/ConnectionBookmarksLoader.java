package com.druvu.jconsole.core.bookmarks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the user-editable {@code connections.txt} bookmarks file into a list of {@link BookmarkGroup}s. Malformed
 * lines are logged at WARN level (with line number) and skipped — never fatal.
 *
 * <p>File grammar:
 *
 * <pre>
 * # comment lines and blank lines are ignored
 * [GROUP NAME]            ← submenu header
 * displayName@url         ← bookmark entry
 * ---                     ← separator within a group
 * </pre>
 */
public final class ConnectionBookmarksLoader {

    private static final System.Logger LOG = System.getLogger(ConnectionBookmarksLoader.class.getName());

    private ConnectionBookmarksLoader() {}

    public static List<BookmarkGroup> parse(Reader in) throws IOException {
        List<BookmarkGroup> groups = new ArrayList<>();
        List<BookmarkEntry> current = null;
        String currentName = null;
        int lineNo = 0;
        try (BufferedReader r = (in instanceof BufferedReader b) ? b : new BufferedReader(in)) {
            String raw;
            while ((raw = r.readLine()) != null) {
                lineNo++;
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]") && line.length() > 2) {
                    if (currentName != null) {
                        groups.add(new BookmarkGroup(currentName, current));
                    }
                    currentName = line.substring(1, line.length() - 1).strip();
                    current = new ArrayList<>();
                    continue;
                }
                if (line.equals("---")) {
                    if (current == null) {
                        LOG.log(
                                System.Logger.Level.WARNING,
                                "connections.txt:" + lineNo + ": separator before any [group] header — skipped");
                        continue;
                    }
                    current.add(BookmarkSeparator.INSTANCE);
                    continue;
                }
                int at = line.indexOf('@');
                if (at <= 0 || at == line.length() - 1) {
                    LOG.log(
                            System.Logger.Level.WARNING,
                            "connections.txt:" + lineNo + ": malformed entry (expected name@url): " + line);
                    continue;
                }
                if (current == null) {
                    LOG.log(
                            System.Logger.Level.WARNING,
                            "connections.txt:" + lineNo + ": bookmark before any [group] header — skipped: " + line);
                    continue;
                }
                String name = line.substring(0, at).strip();
                String url = line.substring(at + 1).strip();
                if (name.isEmpty() || url.isEmpty()) {
                    LOG.log(
                            System.Logger.Level.WARNING,
                            "connections.txt:" + lineNo + ": empty name or url — skipped: " + line);
                    continue;
                }
                current.add(new Bookmark(name, url));
            }
        }
        if (currentName != null) {
            groups.add(new BookmarkGroup(currentName, current));
        }
        return groups;
    }
}
