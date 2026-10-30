package com.druvu.jconsole.ui.menu;

import com.druvu.jconsole.util.BoosterHome;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Writer for the user's {@code connections.txt} bookmarks file, plus opening it in the system editor.
 *
 * <p><b>Additive by design.</b> It only ever inserts one line: hand-authored groups, display markup, comments, ordering
 * and the file's line separator all survive verbatim — editing / deleting / reordering stay the text editor's job (see
 * {@link #openInEditor()}). A bookmark stores only a label and a URL, <b>never credentials</b>.
 *
 * <p>A new bookmark goes at the end of its target group's block, found case-insensitively anywhere in the file; a
 * {@code [group]} header is written only when no such group exists, so a group can never be duplicated.
 *
 * @see ConnectionBookmarksLoader for the file grammar this stays compatible with.
 */
public final class BookmarkWriter {

    /** Group used when the caller does not specify one. */
    public static final String DEFAULT_GROUP = "My Connections";

    private BookmarkWriter() {}

    /** Appends {@code label@url} under {@code group} (or {@link #DEFAULT_GROUP}) to the user's bookmarks file. */
    public static Path appendBookmark(String group, String label, String url) throws IOException {
        return appendBookmark(BoosterHome.connectionsFile(), group, label, url);
    }

    /** Package-visible for tests: append to an explicit file. */
    static Path appendBookmark(Path file, String group, String label, String url) throws IOException {
        String targetGroup = sanitizeGroup((group == null || group.isBlank()) ? DEFAULT_GROUP : group);
        String safeLabel = sanitizeLabel(label);
        String safeUrl = (url == null) ? "" : url.strip();
        if (safeLabel.isEmpty() || safeUrl.isEmpty()) {
            throw new IOException("A bookmark needs a non-empty label and URL");
        }

        String existing = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
        String updated = withEntry(existing, targetGroup, safeLabel + "@" + safeUrl);

        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                file, updated, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return file;
    }

    /**
     * {@code content} with {@code entry} placed at the end of {@code group}'s block, or with a new group appended when
     * the file has no such group. Package-visible for tests.
     *
     * <p>Group lookup is case-insensitive and scans the whole file, not just its tail — a bookmark added to a group
     * that exists earlier in the file must land there instead of appending a second header for the same group. When the
     * group is found no header is written, so the file's own spelling of the name is what survives.
     *
     * <p>Every other line is carried over verbatim, including the file's existing line separator, so comments, ordering
     * and hand-authored markup are untouched.
     */
    static String withEntry(String content, String group, String entry) {
        String nl = lineSeparatorOf(content);
        List<String> lines = new ArrayList<>();
        if (!content.isEmpty()) {
            lines.addAll(Arrays.asList(content.split("\\R", -1)));
            if (lines.getLast().isEmpty()) {
                lines.removeLast(); // trailing newline — re-added by the join below
            }
        }

        int header = indexOfGroupHeader(lines, group);
        if (header < 0) {
            lines.add("[" + group + "]");
            lines.add(entry);
        } else {
            lines.add(endOfGroupBlock(lines, header), entry);
        }
        return String.join(nl, lines) + nl;
    }

    /** Opens the bookmarks file in the OS text editor, falling back to the default open action. */
    public static void openInEditor() throws IOException {
        Path file = BoosterHome.connectionsFile();
        if (!Files.exists(file)) {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, "", StandardOpenOption.CREATE);
        }
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("Desktop actions are not supported on this platform");
        }
        Desktop desktop = Desktop.getDesktop();
        File f = file.toFile();
        if (desktop.isSupported(Desktop.Action.EDIT)) {
            try {
                desktop.edit(f);
                return;
            } catch (IOException editFailed) {
                // some platforms advertise EDIT but cannot honour it — fall back to OPEN
            }
        }
        if (desktop.isSupported(Desktop.Action.OPEN)) {
            desktop.open(f);
        } else {
            throw new IOException("Neither EDIT nor OPEN is supported for " + file);
        }
    }

    /** Index of the first {@code [group]} header naming {@code group} (case-insensitive), or {@code -1}. */
    private static int indexOfGroupHeader(List<String> lines, String group) {
        for (int i = 0; i < lines.size(); i++) {
            String name = groupNameOf(lines.get(i));
            if (name != null && name.equalsIgnoreCase(group)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Index just past the last content line of the group opened at {@code header} — where a new entry belongs. Trailing
     * blank lines are left below the insertion point so the blank line separating groups stays put.
     */
    private static int endOfGroupBlock(List<String> lines, int header) {
        int end = lines.size();
        for (int i = header + 1; i < lines.size(); i++) {
            if (groupNameOf(lines.get(i)) != null) {
                end = i;
                break;
            }
        }
        while (end > header + 1 && lines.get(end - 1).isBlank()) {
            end--;
        }
        return end;
    }

    /**
     * The group name if {@code line} is a {@code [header]}, else {@code null}. Matches
     * {@link ConnectionBookmarksLoader}'s rule, so a bookmark whose label starts with colour markup (e.g. {@code [red
     * prod-1]@host:1}) is not mistaken for a header — it does not end with {@code ]}.
     */
    private static String groupNameOf(String line) {
        String s = line.strip();
        if (s.length() > 2 && s.startsWith("[") && s.endsWith("]")) {
            return s.substring(1, s.length() - 1).strip();
        }
        return null;
    }

    /** The separator the file already uses, so rewriting does not flip every line ending on Windows. */
    private static String lineSeparatorOf(String content) {
        if (content.contains("\r\n")) {
            return "\r\n";
        }
        return content.contains("\n") ? "\n" : System.lineSeparator();
    }

    private static String sanitizeGroup(String group) {
        // Group names are wrapped in [ ]; keep them single-line and bracket-free.
        return group.strip()
                .replace('[', ' ')
                .replace(']', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .strip();
    }

    private static String sanitizeLabel(String label) {
        // '@' is the label/url delimiter and a newline ends the entry — keep the label to one field on one line.
        // Display markup (*bold*, [color text]) is intentionally preserved.
        return (label == null)
                ? ""
                : label.strip()
                        .replace('@', ' ')
                        .replace('\n', ' ')
                        .replace('\r', ' ')
                        .strip();
    }
}
