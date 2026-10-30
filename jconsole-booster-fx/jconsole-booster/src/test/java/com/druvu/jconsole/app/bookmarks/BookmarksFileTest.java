package com.druvu.jconsole.app.bookmarks;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.core.bookmarks.BookmarkGroup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Covers the bookmarks file: the default seeded when missing, group names, and labels by target. */
public class BookmarksFileTest {

    private Path home;
    private Path file;

    @BeforeMethod
    public void createHome() throws IOException {
        home = Files.createTempDirectory("jcb-bookmarks");
        file = home.resolve("connections.txt");
    }

    @AfterMethod(alwaysRun = true)
    public void deleteHome() throws IOException {
        Files.deleteIfExists(file);
        Files.deleteIfExists(home);
    }

    @Test
    public void aMissingFileIsSeededWithTheDefault() {
        List<BookmarkGroup> groups = new BookmarksFile(file).groups();

        assertThat(file).exists();
        assertThat(groups).extracting(BookmarkGroup::name).containsExactly("Local", "Examples");
    }

    @Test
    public void groupNamesAreInFileOrderEachOnce() throws IOException {
        Files.writeString(file, """
                [Prod]
                a@a:1
                [Staging]
                b@b:1
                [prod]
                c@c:1
                """);

        assertThat(new BookmarksFile(file).groupNames()).containsExactly("Prod", "Staging", "prod");
    }

    @Test
    public void labelsArePlainAndKeyedByTheCanonicalTarget() throws IOException {
        Files.writeString(file, """
                [Prod]
                [red prod-1]@prod-1.example.com:7091
                *london*@service:jmx:jmxmp://london.example.com:7091
                ---
                again@prod-1.example.com:7091
                """);

        Map<String, String> labels = new BookmarksFile(file).labelsByTarget();

        assertThat(labels)
                .containsExactly(
                        Map.entry("prod-1.example.com:7091", "prod-1"), Map.entry("london.example.com:7091", "london"));
    }

    @Test
    public void ensureExistsCreatesAnEmptyFileOnlyWhenMissing() throws IOException {
        BookmarksFile bookmarks = new BookmarksFile(file);

        bookmarks.ensureExists();
        assertThat(file).isEmptyFile();

        Files.writeString(file, "[Mine]\n");
        bookmarks.ensureExists();
        assertThat(file).hasContent("[Mine]");
    }
}
