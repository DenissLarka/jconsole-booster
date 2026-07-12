package com.druvu.jconsole.ui.menu;

import com.druvu.jconsole.launcher.JConsole;
import com.druvu.jconsole.util.BoosterHome;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds the "Bookmarks" submenu added to the Connection menu. On click, each bookmark prefills the Connect dialog with
 * its URL via {@link JConsole#promptConnect} — it does not connect immediately, because bookmarks store no credentials
 * and a credentialed host needs the operator to enter a password first.
 *
 * <p>The {@code connections.txt} file is loaded from {@link BoosterHome#connectionsFile()}; if it does not exist on
 * launch, a documented default is copied into place from the bundled resource.
 */
@Slf4j
public final class ConnectionBookmarksMenu {

    private static final String BUNDLED_DEFAULT_RESOURCE = "/com/druvu/jconsole/ui/menu/connections-default.txt";

    private ConnectionBookmarksMenu() {}

    /**
     * Builds a {@link JMenu} populated with the bookmarks defined in the user's {@code connections.txt}. If the file is
     * missing, the bundled default is copied to {@link BoosterHome#connectionsFile()} first.
     *
     * @param title the menu title (e.g. "Bookmarks")
     * @param onPick callback for the actual connection — production caller should pass {@code (url) ->
     *     jconsole.addUrl(url, null, null, false)}; tests can pass any handler.
     */
    public static JMenu build(String title, BookmarkClickHandler onPick) {
        JMenu menu = new JMenu(title);
        populate(menu, onPick);
        return menu;
    }

    /** Clears {@code menu} and (re)populates it from {@code connections.txt} — call to refresh after an add/edit. */
    public static void populate(JMenu menu, BookmarkClickHandler onPick) {
        menu.removeAll();
        List<BookmarkGroup> groups = loadGroups();
        if (groups.isEmpty()) {
            JMenuItem empty = new JMenuItem("(no bookmarks — edit connections.txt)");
            empty.setEnabled(false);
            menu.add(empty);
            return;
        }
        for (BookmarkGroup group : groups) {
            JMenu submenu = new JMenu(group.name());
            for (BookmarkEntry entry : group.entries()) {
                if (entry instanceof BookmarkSeparator) {
                    submenu.addSeparator();
                } else if (entry instanceof Bookmark b) {
                    JMenuItem item = new JMenuItem(MarkupRenderer.render(b.displayName()));
                    item.addActionListener(e -> onPick.connect(b.url()));
                    submenu.add(item);
                }
            }
            menu.add(submenu);
        }
    }

    /**
     * Click handler that prefills the Connect dialog with the bookmarked URL rather than connecting immediately.
     * Bookmarks store no credentials (by design), so a one-click connect would fail on any host that needs a password;
     * prefilling lets the operator enter credentials and connect.
     */
    public static BookmarkClickHandler defaultHandler(JConsole jconsole) {
        return jconsole::promptConnect;
    }

    private static List<BookmarkGroup> loadGroups() {
        Path file = BoosterHome.connectionsFile();
        if (!Files.exists(file)) {
            try {
                copyBundledDefault(file);
            } catch (IOException e) {
                log.warn("Could not seed default connections.txt at {}: {}", file, e.getMessage());
                return List.of();
            }
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return ConnectionBookmarksLoader.parse(r);
        } catch (IOException e) {
            log.warn("Could not read connections.txt at {}: {}", file, e.getMessage());
            return List.of();
        }
    }

    private static void copyBundledDefault(Path target) throws IOException {
        try (InputStream in = ConnectionBookmarksMenu.class.getResourceAsStream(BUNDLED_DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IOException("bundled resource " + BUNDLED_DEFAULT_RESOURCE + " not found");
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
            log.info("Seeded default connections.txt at {}", target);
        }
    }

    @FunctionalInterface
    public interface BookmarkClickHandler {
        void connect(String url);
    }
}
