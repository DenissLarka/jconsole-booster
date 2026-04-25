package com.druvu.jconsole.ui.menu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public final class ConnectionBookmarksLoader {

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
                        log.warn("connections.txt:{}: separator before any [group] header — skipped", lineNo);
                        continue;
                    }
                    current.add(BookmarkSeparator.INSTANCE);
                    continue;
                }
                int at = line.indexOf('@');
                if (at <= 0 || at == line.length() - 1) {
                    log.warn("connections.txt:{}: malformed entry (expected name@url): {}", lineNo, line);
                    continue;
                }
                if (current == null) {
                    log.warn("connections.txt:{}: bookmark before any [group] header — skipped: {}", lineNo, line);
                    continue;
                }
                String name = line.substring(0, at).strip();
                String url = line.substring(at + 1).strip();
                if (name.isEmpty() || url.isEmpty()) {
                    log.warn("connections.txt:{}: empty name or url — skipped: {}", lineNo, line);
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
