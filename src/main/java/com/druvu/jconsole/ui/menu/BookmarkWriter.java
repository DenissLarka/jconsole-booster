package com.druvu.jconsole.ui.menu;

import com.druvu.jconsole.util.BoosterHome;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Append-only writer for the user's {@code connections.txt} bookmarks file, plus opening it in the system editor.
 *
 * <p><b>Append-only by design.</b> It never rewrites existing content, so hand-authored groups, display markup,
 * comments and ordering survive verbatim — editing / deleting / reordering stay the text editor's job (see
 * {@link #openInEditor()}). A bookmark stores only a label and a URL, <b>never credentials</b>. A new bookmark is
 * appended under a target group; a fresh {@code [group]} header is written only when the file does not already end
 * inside that group, so repeated adds to the same group do not duplicate the header.
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
        StringBuilder out = new StringBuilder();
        if (!existing.isEmpty() && !existing.endsWith("\n") && !existing.endsWith("\r")) {
            out.append(System.lineSeparator()); // never glue onto an unterminated last line
        }
        if (!endsInGroup(existing, targetGroup)) {
            out.append('[').append(targetGroup).append(']').append(System.lineSeparator());
        }
        out.append(safeLabel).append('@').append(safeUrl).append(System.lineSeparator());

        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                file, out.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return file;
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

    /** @return {@code true} if the last {@code [group]} header in {@code content} names {@code group}. */
    private static boolean endsInGroup(String content, String group) {
        String last = null;
        for (String raw : content.split("\\R", -1)) {
            String line = raw.strip();
            if (line.startsWith("[") && line.endsWith("]") && line.length() > 2) {
                last = line.substring(1, line.length() - 1).strip();
            }
        }
        return group.equals(last);
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
