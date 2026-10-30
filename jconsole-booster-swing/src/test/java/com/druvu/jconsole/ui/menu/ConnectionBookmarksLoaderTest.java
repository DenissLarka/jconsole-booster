package com.druvu.jconsole.ui.menu;

import java.io.StringReader;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConnectionBookmarksLoaderTest {

    @Test
    public void parsesGroupsItemsAndSeparator() throws Exception {
        String src = """
                # JConsole Booster bookmarks

                [Local]
                lo@localhost:7091
                ---
                rmi-local@rmi:localhost:9999

                [Prod]
                p1@prod-1:7091
                """;

        List<BookmarkGroup> groups = ConnectionBookmarksLoader.parse(new StringReader(src));

        Assert.assertEquals(groups.size(), 2);
        Assert.assertEquals(groups.get(0).name(), "Local");
        Assert.assertEquals(groups.get(0).entries().size(), 3);
        Assert.assertTrue(groups.get(0).entries().get(0) instanceof Bookmark);
        Assert.assertSame(groups.get(0).entries().get(1), BookmarkSeparator.INSTANCE);
        Assert.assertTrue(groups.get(0).entries().get(2) instanceof Bookmark);

        Bookmark first = (Bookmark) groups.get(0).entries().get(0);
        Assert.assertEquals(first.displayName(), "lo");
        Assert.assertEquals(first.url(), "localhost:7091");

        Assert.assertEquals(groups.get(1).name(), "Prod");
        Assert.assertEquals(groups.get(1).entries().size(), 1);
    }

    @Test
    public void emptyInputProducesNoGroups() throws Exception {
        List<BookmarkGroup> groups = ConnectionBookmarksLoader.parse(new StringReader(""));
        Assert.assertEquals(groups, List.of());
    }

    @Test
    public void commentsAndBlanksAreIgnored() throws Exception {
        String src = """
                # comment
                #another
                [G]


                a@x:1
                """;
        List<BookmarkGroup> groups = ConnectionBookmarksLoader.parse(new StringReader(src));
        Assert.assertEquals(groups.size(), 1);
        Assert.assertEquals(groups.get(0).entries().size(), 1);
    }

    @Test
    public void malformedLinesAreSkippedNotFatal() throws Exception {
        String src = """
                [G]
                no-at-sign
                @missing-name
                missing-url@
                ok@host:1
                """;
        List<BookmarkGroup> groups = ConnectionBookmarksLoader.parse(new StringReader(src));
        Assert.assertEquals(groups.size(), 1);
        Assert.assertEquals(groups.get(0).entries().size(), 1);
        Assert.assertEquals(((Bookmark) groups.get(0).entries().get(0)).displayName(), "ok");
    }

    @Test
    public void entryBeforeAnyGroupIsSkipped() throws Exception {
        String src = """
                orphan@host:1
                [G]
                in-group@host:2
                """;
        List<BookmarkGroup> groups = ConnectionBookmarksLoader.parse(new StringReader(src));
        Assert.assertEquals(groups.size(), 1);
        Assert.assertEquals(groups.get(0).entries().size(), 1);
        Assert.assertEquals(((Bookmark) groups.get(0).entries().get(0)).displayName(), "in-group");
    }

    @Test
    public void separatorBeforeAnyGroupIsSkipped() throws Exception {
        String src = """
                ---
                [G]
                a@host:1
                """;
        List<BookmarkGroup> groups = ConnectionBookmarksLoader.parse(new StringReader(src));
        Assert.assertEquals(groups.size(), 1);
        Assert.assertEquals(groups.get(0).entries().size(), 1);
    }

    @Test
    public void displayNameWithMarkupIsPreservedVerbatim() throws Exception {
        // The loader does not interpret markup — that is MarkupRenderer's job.
        // The raw display name (markup intact) must round-trip from file to record.
        String src = """
                [G]
                *bold*@h:1
                [red ALERT]@h:2
                """;
        List<BookmarkGroup> groups = ConnectionBookmarksLoader.parse(new StringReader(src));
        Assert.assertEquals(((Bookmark) groups.get(0).entries().get(0)).displayName(), "*bold*");
        Assert.assertEquals(((Bookmark) groups.get(0).entries().get(1)).displayName(), "[red ALERT]");
    }
}
