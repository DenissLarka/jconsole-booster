package com.druvu.jconsole.app.bookmarks;

import com.druvu.jconsole.app.connect.ConnectionRequest;
import com.druvu.jconsole.core.bookmarks.Bookmark;
import com.druvu.jconsole.core.bookmarks.BookmarkEntry;
import com.druvu.jconsole.core.bookmarks.BookmarkGroup;
import com.druvu.jconsole.core.bookmarks.ConnectionBookmarksLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * The user's bookmarks file ({@code connections.txt}): read for the Bookmarks menu, the group picker and the Connect
 * dialog's labels. A missing file is seeded with a documented default on first read. Reading never fails the caller: an
 * unreadable file reads as no bookmarks.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@Slf4j
public final class BookmarksFile {

    private static final String BUNDLED_DEFAULT = "connections-default.txt";

    private final Path file;

    /** @param file the bookmarks file */
    public BookmarksFile(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public Path path() {
        return file;
    }

    /** The bookmark groups in file order; the default file is written first when there is none. */
    public List<BookmarkGroup> groups() {
        if (!Files.exists(file)) {
            try {
                seedDefault();
            } catch (IOException e) {
                log.warn("Could not write the default bookmarks to {}: {}", file, e.getMessage());
                return List.of();
            }
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return ConnectionBookmarksLoader.parse(reader);
        } catch (IOException e) {
            log.warn("Could not read the bookmarks in {}: {}", file, e.getMessage());
            return List.of();
        }
    }

    /** The group names in file order, each once: what the Add bookmark dialog offers. */
    public List<String> groupNames() {
        return groups().stream()
                .map(BookmarkGroup::name)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
    }

    /**
     * Every bookmark's plain label by its target in {@link ConnectionRequest#canonical} form, so a bookmark written as
     * a full service URL still matches the {@code host:port} a recent connection is kept as. The first bookmark of a
     * target wins: the same host may be in several groups.
     */
    public Map<String, String> labelsByTarget() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (BookmarkGroup group : groups()) {
            for (BookmarkEntry entry : group.entries()) {
                if (entry instanceof Bookmark bookmark) {
                    String target = ConnectionRequest.canonical(bookmark.url());
                    String label = BookmarkMarkup.plain(bookmark.displayName()).strip();
                    if (!target.isEmpty() && !label.isEmpty()) {
                        labels.putIfAbsent(target, label);
                    }
                }
            }
        }
        return labels;
    }

    /** The file, created empty when there is none, so an editor can open it. */
    public Path ensureExists() throws IOException {
        if (!Files.exists(file)) {
            createParentDirectory();
            Files.writeString(file, "");
        }
        return file;
    }

    private void seedDefault() throws IOException {
        try (InputStream bundled = BookmarksFile.class.getResourceAsStream(BUNDLED_DEFAULT)) {
            if (bundled == null) {
                throw new IOException("the bundled " + BUNDLED_DEFAULT + " is missing");
            }
            createParentDirectory();
            Files.copy(bundled, file);
            log.info("Wrote the default bookmarks to {}", file);
        }
    }

    private void createParentDirectory() throws IOException {
        Path directory = file.toAbsolutePath().getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }
    }
}
