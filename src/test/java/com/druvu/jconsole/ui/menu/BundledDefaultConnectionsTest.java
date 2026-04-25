package com.druvu.jconsole.ui.menu;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BundledDefaultConnectionsTest {

    @Test
    public void bundledDefaultParsesCleanly() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(
                "/com/druvu/jconsole/ui/menu/connections-default.txt")) {
            Assert.assertNotNull(in, "bundled connections-default.txt resource missing");
            List<BookmarkGroup> groups = ConnectionBookmarksLoader.parse(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            Assert.assertFalse(groups.isEmpty(), "bundled default should contain at least one group");
            for (BookmarkGroup g : groups) {
                Assert.assertNotNull(g.name());
                Assert.assertFalse(g.name().isBlank());
                for (BookmarkEntry e : g.entries()) {
                    if (e instanceof Bookmark b) {
                        Assert.assertFalse(b.displayName().isBlank());
                        Assert.assertFalse(b.url().isBlank());
                    }
                }
            }
        }
    }
}
