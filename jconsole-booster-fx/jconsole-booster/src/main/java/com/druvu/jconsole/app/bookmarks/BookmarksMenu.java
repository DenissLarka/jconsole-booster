package com.druvu.jconsole.app.bookmarks;

import com.druvu.jconsole.core.bookmarks.Bookmark;
import com.druvu.jconsole.core.bookmarks.BookmarkEntry;
import com.druvu.jconsole.core.bookmarks.BookmarkGroup;
import com.druvu.jconsole.core.bookmarks.BookmarkSeparator;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Fills the Bookmarks menu: one submenu per group, one item per bookmark. A click hands the bookmark's target on; it
 * never connects by itself, because a bookmark keeps no password.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class BookmarksMenu {

    static final String EMPTY = "(no bookmarks: edit connections.txt)";

    private BookmarksMenu() {}

    /**
     * Replaces the menu's items with the groups' bookmarks.
     *
     * @param onPick told the target of the clicked bookmark
     */
    public static void fill(Menu menu, List<BookmarkGroup> groups, Consumer<String> onPick) {
        menu.getItems().clear();
        if (groups.isEmpty()) {
            MenuItem empty = new MenuItem(EMPTY);
            empty.setDisable(true);
            menu.getItems().add(empty);
            return;
        }
        for (BookmarkGroup group : groups) {
            Menu groupMenu = new Menu(group.name());
            for (BookmarkEntry entry : group.entries()) {
                if (entry instanceof BookmarkSeparator) {
                    groupMenu.getItems().add(new SeparatorMenuItem());
                } else if (entry instanceof Bookmark bookmark) {
                    MenuItem item = new MenuItem(BookmarkMarkup.plain(bookmark.displayName()));
                    item.setOnAction(event -> onPick.accept(bookmark.url()));
                    groupMenu.getItems().add(item);
                }
            }
            menu.getItems().add(groupMenu);
        }
    }
}
