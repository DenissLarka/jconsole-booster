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

    @Test
    public void addsToAGroupThatIsNotTheLastOneInTheFile() throws IOException {
        Path f = freshTempFile();
        try {
            Files.writeString(
                    f,
                    "[Local]\nlocal sample@localhost:7091\n\n[Examples]\n*london*@london.example.com:7091\n",
                    StandardCharsets.UTF_8);
            BookmarkWriter.appendBookmark(f, "Local", "another", "localhost:60155");

            List<BookmarkGroup> groups = parse(f);
            Assert.assertEquals(groups.size(), 2, "[Local] must not be duplicated at the end of the file");
            Assert.assertEquals(groups.get(0).name(), "Local");
            Assert.assertEquals(groups.get(0).entries().size(), 2, "the new entry belongs in the existing group");
            Assert.assertEquals(groups.get(1).name(), "Examples");
            Assert.assertEquals(groups.get(1).entries().size(), 1, "the following group must be untouched");
        } finally {
            Files.deleteIfExists(f);
        }
    }

    @Test
    public void groupLookupIsCaseInsensitiveAndKeepsTheFilesSpelling() throws IOException {
        Path f = freshTempFile();
        try {
            Files.writeString(f, "[Local]\nlocal sample@localhost:7091\n", StandardCharsets.UTF_8);
            BookmarkWriter.appendBookmark(f, "local", "another", "host:2");

            String content = Files.readString(f, StandardCharsets.UTF_8);
            Assert.assertTrue(content.contains("[Local]"), "the file's own spelling must survive");
            Assert.assertFalse(content.contains("[local]"), "a case variant must not create a second group");
            Assert.assertEquals(parse(f).size(), 1);
        } finally {
            Files.deleteIfExists(f);
        }
    }

    @Test
    public void entryLandsInsideTheGroupNotAfterTheBlankLineSeparatingGroups() {
        String content = "[A]\nx@h:1\n\n[B]\ny@h:2\n";

        String out = BookmarkWriter.withEntry(content, "A", "z@h:3");

        Assert.assertEquals(out, "[A]\nx@h:1\nz@h:3\n\n[B]\ny@h:2\n");
    }

    @Test
    public void existingLineSeparatorIsPreserved() {
        String crlf = "[A]\r\nx@h:1\r\n";

        String out = BookmarkWriter.withEntry(crlf, "A", "z@h:3");

        Assert.assertEquals(out, "[A]\r\nx@h:1\r\nz@h:3\r\n");
        Assert.assertFalse(out.contains("\n\n"), "no stray bare newline may be introduced");
    }

    @Test
    public void aLabelStartingWithColorMarkupIsNotMistakenForAGroupHeader() {
        // "[red prod-1]@host:1" starts with '[' but is an entry, not a header.
        String content = "[Prod]\n[red prod-1]@prod-1.example.com:7091\n";

        String out = BookmarkWriter.withEntry(content, "Prod", "new@host:9");

        Assert.assertEquals(out, "[Prod]\n[red prod-1]@prod-1.example.com:7091\nnew@host:9\n");
    }

    @Test
    public void emptyLabelOrUrlIsRejected() throws IOException {
        Path f = freshTempFile();
        try {
            Assert.assertThrows(IOException.class, () -> BookmarkWriter.appendBookmark(f, "G", "  ", "host:1"));
            Assert.assertThrows(IOException.class, () -> BookmarkWriter.appendBookmark(f, "G", "label", " "));
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
