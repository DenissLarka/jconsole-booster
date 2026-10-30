package com.druvu.jconsole.ui.menu;

import java.io.StringReader;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the URL → label mapping behind the Connect dialog's labeled history: markup is stripped, the short and full
 * URL forms match each other, and a host listed in several groups resolves to one label.
 */
public class BookmarkLabelsTest {

    private static Map<String, String> labelsOf(String connectionsFile) throws Exception {
        return BookmarkLabels.byUrl(ConnectionBookmarksLoader.parse(new StringReader(connectionsFile)));
    }

    @Test
    public void mapsUrlToPlainLabelWithMarkupStripped() throws Exception {
        Map<String, String> labels = labelsOf("""
                [Local]
                local connection sample@localhost:7091

                [Prod]
                [red prod-1]@prod-1.example.com:7091
                *stg*@stg.example.com:7091
                """);

        Assert.assertEquals(labels.get("localhost:7091"), "local connection sample");
        Assert.assertEquals(labels.get("prod-1.example.com:7091"), "prod-1");
        Assert.assertEquals(labels.get("stg.example.com:7091"), "stg");
    }

    @Test
    public void matchesAcrossShortAndFullUrlForms() throws Exception {
        // Hand-authored as a full service URL; the MRU records the short form. They must land on the same key.
        Map<String, String> labels = labelsOf("""
                [Local]
                lo@service:jmx:jmxmp://localhost:7091
                """);

        Assert.assertEquals(labels.get(BookmarkLabels.canonical("localhost:7091")), "lo");
    }

    @Test
    public void firstBookmarkWinsWhenAHostIsListedTwice() throws Exception {
        Map<String, String> labels = labelsOf("""
                [Primary]
                the real name@localhost:7091

                [Duplicates]
                another name@localhost:7091
                """);

        Assert.assertEquals(labels.get("localhost:7091"), "the real name");
    }

    @Test
    public void canonicalIsNullAndBlankSafe() {
        Assert.assertEquals(BookmarkLabels.canonical(null), "");
        Assert.assertEquals(BookmarkLabels.canonical("   "), "");
    }
}
