package com.druvu.jconsole.app.bookmarks;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** Covers a bookmark label as plain text: bold and known colours unwrapped, an unknown colour left as written. */
public class BookmarkMarkupTest {

    @Test
    public void markupBecomesItsInnerText() {
        assertThat(BookmarkMarkup.plain("*london-prod*")).isEqualTo("london-prod");
        assertThat(BookmarkMarkup.plain("[red prod-1]")).isEqualTo("prod-1");
        assertThat(BookmarkMarkup.plain("[Orange staging 1] and *b*")).isEqualTo("staging 1 and b");
    }

    @Test
    public void anUnknownColourStaysAsWritten() {
        assertThat(BookmarkMarkup.plain("[pink x]")).isEqualTo("[pink x]");
    }

    @Test
    public void plainTextAndNothingStayAsTheyAre() {
        assertThat(BookmarkMarkup.plain("local sample target")).isEqualTo("local sample target");
        assertThat(BookmarkMarkup.plain(null)).isEmpty();
    }
}
