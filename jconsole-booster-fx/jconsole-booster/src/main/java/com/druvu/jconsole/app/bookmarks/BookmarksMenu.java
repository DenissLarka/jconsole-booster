package com.druvu.jconsole.app.bookmarks;

import com.druvu.jconsole.core.bookmarks.Bookmark;
import com.druvu.jconsole.core.bookmarks.BookmarkEntry;
import com.druvu.jconsole.core.bookmarks.BookmarkGroup;
import com.druvu.jconsole.core.bookmarks.BookmarkSeparator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * The bookmarks as menu items, for the toolbar's connection list: one submenu per group, one item per bookmark. A click
 * hands the bookmark's target on; it never connects by itself, because a bookmark keeps no password.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class BookmarksMenu {

    static final String EMPTY = "(no bookmarks: edit connections.txt)";

    private BookmarksMenu() {}

    /**
     * The groups' bookmarks as items: a submenu per group, or one disabled line saying there are none.
     *
     * @param onPick told the target of the clicked bookmark
     */
    public static List<MenuItem> items(List<BookmarkGroup> groups, Consumer<String> onPick) {
        if (groups.isEmpty()) {
            MenuItem empty = new MenuItem(EMPTY);
            empty.setDisable(true);
            return List.of(empty);
        }
        List<MenuItem> items = new ArrayList<>();
        for (BookmarkGroup group : groups) {
            Menu groupMenu = new Menu(group.name());
            // the user's names: an "_" in them is text, not the mark of a keyboard shortcut
            groupMenu.setMnemonicParsing(false);
            for (BookmarkEntry entry : group.entries()) {
                if (entry instanceof BookmarkSeparator) {
                    groupMenu.getItems().add(new SeparatorMenuItem());
                } else if (entry instanceof Bookmark bookmark) {
                    MenuItem item = new MenuItem(BookmarkMarkup.plain(bookmark.displayName()));
                    item.setMnemonicParsing(false);
                    item.setOnAction(event -> onPick.accept(bookmark.url()));
                    groupMenu.getItems().add(item);
                }
            }
            items.add(groupMenu);
        }
        return items;
    }
}
