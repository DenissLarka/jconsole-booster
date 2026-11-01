package com.druvu.jconsole.app.bookmarks;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.FxTestToolkit;
import com.druvu.jconsole.core.bookmarks.Bookmark;
import com.druvu.jconsole.core.bookmarks.BookmarkGroup;
import com.druvu.jconsole.core.bookmarks.BookmarkSeparator;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Covers the bookmarks in the connection list: a submenu per group, plain labels, separators, a click hands the target
 * on.
 */
public class BookmarksMenuTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void theItemsWithoutBookmarksAreOneDisabledLine() {
        List<MenuItem> items = BookmarksMenu.items(List.of(), target -> {});

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.getText()).isEqualTo(BookmarksMenu.EMPTY);
            assertThat(item.isDisable()).isTrue();
        });
    }

    @Test
    public void aGroupIsASubmenuAndABookmarkClickHandsItsTargetOn() {
        List<String> picked = new ArrayList<>();
        List<BookmarkGroup> groups = List.of(new BookmarkGroup(
                "Prod",
                List.of(
                        new Bookmark("[red prod-1]", "prod-1:7091"),
                        BookmarkSeparator.INSTANCE,
                        new Bookmark("b", "b:1"))));

        Menu prod = FxTestToolkit.call(() -> {
            Menu group = (Menu) BookmarksMenu.items(groups, picked::add).getFirst();
            group.getItems().getFirst().fire();
            return group;
        });

        assertThat(prod.getText()).isEqualTo("Prod");
        assertThat(prod.getItems()).extracting(MenuItem::getText).containsExactly("prod-1", null, "b");
        assertThat(prod.getItems().get(1)).isInstanceOf(SeparatorMenuItem.class);
        assertThat(picked).containsExactly("prod-1:7091");
    }
}
