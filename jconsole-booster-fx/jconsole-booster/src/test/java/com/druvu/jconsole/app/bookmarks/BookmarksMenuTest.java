package com.druvu.jconsole.app.bookmarks;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.FxTestToolkit;
import com.druvu.jconsole.core.bookmarks.Bookmark;
import com.druvu.jconsole.core.bookmarks.BookmarkGroup;
import com.druvu.jconsole.core.bookmarks.BookmarkSeparator;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Covers the bookmarks in the connection list: a submenu per group, labels bold or coloured as marked up, separators, a
 * click hands the target on.
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
        assertThat(prod.getItems()).extracting(BookmarksMenuTest::shownText).containsExactly("prod-1", null, "b");
        assertThat(prod.getItems().get(1)).isInstanceOf(SeparatorMenuItem.class);
        assertThat(picked).containsExactly("prod-1:7091");
    }

    /** 1.1's markup in the list: a bold label shows bold, a coloured one in its colour, a plain one as it is. */
    @Test
    public void markedUpLabelsShowBoldAndInTheirColour() {
        List<BookmarkGroup> groups = List.of(new BookmarkGroup(
                "Prod",
                List.of(
                        new Bookmark("*london-prod*", "a:1"),
                        new Bookmark("[red prod-1]", "b:1"),
                        new Bookmark("local", "c:1"))));

        List<List<String>> shown = FxTestToolkit.call(() -> {
            Menu group = (Menu) BookmarksMenu.items(groups, target -> {}).getFirst();
            return group.getItems().stream().map(BookmarksMenuTest::looks).toList();
        });

        assertThat(shown).containsExactly(List.of("london-prod bold"), List.of("prod-1 #c83232"), List.of("local"));
    }

    /** A menu reads "_" as the mark of a keyboard shortcut and hides it: in a name, it must stay text. */
    @Test
    public void underscoresInGroupsAndBookmarksStayText() {
        List<BookmarkGroup> groups =
                List.of(new BookmarkGroup("my_team", List.of(new Bookmark("prod_eu_1", "prod-eu-1:7091"))));

        Menu group = FxTestToolkit.call(
                () -> (Menu) BookmarksMenu.items(groups, target -> {}).getFirst());

        assertThat(group.isMnemonicParsing()).as("the group's name").isFalse();
        assertThat(group.getItems().getFirst().isMnemonicParsing())
                .as("the bookmark's name")
                .isFalse();
    }

    /** What an item shows, a piece per styled part: "text", then " bold" and the colour when it has them. */
    private static List<String> looks(MenuItem item) {
        if (!(item.getGraphic() instanceof HBox parts)) {
            return List.of(item.getText());
        }
        List<String> looks = new ArrayList<>();
        for (Node part : parts.getChildren()) {
            Label label = (Label) part;
            String style = label.getStyle();
            Matcher color = Pattern.compile("-fx-text-fill: (#\\w+)").matcher(style);
            looks.add(label.getText()
                    + (style.contains("-fx-font-weight: bold") ? " bold" : "")
                    + (color.find() ? " " + color.group(1) : ""));
        }
        return looks;
    }

    /** The text an item shows, styled or not; null for a separator. */
    private static String shownText(MenuItem item) {
        if (!(item.getGraphic() instanceof HBox parts)) {
            return item.getText();
        }
        return parts.getChildren().stream()
                .map(part -> ((Label) part).getText())
                .collect(Collectors.joining());
    }
}
