package com.druvu.jconsole.ui.menu;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests for {@link BookmarkWriter} — appended entries must stay loadable by {@link ConnectionBookmarksLoader}. */
public class BookmarkWriterTest {

    @Test
    public void appendsUnderGroupAndIsLoadable() throws IOException {
        Path f = freshTempFile();
        try {
            BookmarkWriter.appendBookmark(f, "My Servers", "prod-1", "prod.example.com:9010");
            List<BookmarkGroup> groups = parse(f);
            Assert.assertEquals(groups.size(), 1);
            Assert.assertEquals(groups.get(0).name(), "My Servers");
            Bookmark b = (Bookmark) groups.get(0).entries().get(0);
            Assert.assertEquals(b.displayName(), "prod-1");
            Assert.assertEquals(b.url(), "prod.example.com:9010");
        } finally {
            Files.deleteIfExists(f);
        }
    }

    @Test
    public void repeatedAddsToSameGroupDoNotDuplicateTheHeader() throws IOException {
        Path f = freshTempFile();
        try {
            BookmarkWriter.appendBookmark(f, "G", "a", "h1:1");
            BookmarkWriter.appendBookmark(f, "G", "b", "h2:2");
            List<BookmarkGroup> groups = parse(f);
            Assert.assertEquals(groups.size(), 1, "same group must not be duplicated");
            Assert.assertEquals(groups.get(0).entries().size(), 2);
        } finally {
            Files.deleteIfExists(f);
        }
    }

    @Test
    public void aDifferentGroupAddsANewSection() throws IOException {
        Path f = freshTempFile();
        try {
            BookmarkWriter.appendBookmark(f, "G", "a", "h1:1");
            BookmarkWriter.appendBookmark(f, "H", "b", "h2:2");
            Assert.assertEquals(parse(f).size(), 2);
        } finally {
            Files.deleteIfExists(f);
        }
    }

    @Test
    public void atSignInLabelIsSanitizedSoTheEntryStaysWellFormed() throws IOException {
        Path f = freshTempFile();
        try {
            BookmarkWriter.appendBookmark(f, "G", "user@host label", "real.host:1");
            Bookmark b = (Bookmark) parse(f).get(0).entries().get(0);
            Assert.assertFalse(b.displayName().contains("@"), "label must not keep the '@' delimiter");
            Assert.assertEquals(b.url(), "real.host:1");
        } finally {
            Files.deleteIfExists(f);
        }
    }

    @Test
    public void appendingPreservesExistingHandAuthoredContent() throws IOException {
        Path f = freshTempFile();
        try {
            Files.writeString(f, "# my notes\n[Local]\n*server*@localhost:7091\n", StandardCharsets.UTF_8);
            BookmarkWriter.appendBookmark(f, "Local", "another", "host:2");
            String content = Files.readString(f, StandardCharsets.UTF_8);
            Assert.assertTrue(content.contains("# my notes"), "comments must be preserved");
            Assert.assertTrue(content.contains("*server*@localhost:7091"), "existing markup entry must be preserved");
            // Ends inside [Local] already → no duplicate header, single group with both entries.
            List<BookmarkGroup> groups = parse(f);
            Assert.assertEquals(groups.size(), 1);
            Assert.assertEquals(groups.get(0).entries().size(), 2);
        } finally {
            Files.deleteIfExists(f);
        }
    }

    private static Path freshTempFile() throws IOException {
        Path f = Files.createTempFile("connections", ".txt");
        Files.deleteIfExists(f);
        return f;
    }

    private static List<BookmarkGroup> parse(Path f) throws IOException {
        return ConnectionBookmarksLoader.parse(new StringReader(Files.readString(f, StandardCharsets.UTF_8)));
    }
}
