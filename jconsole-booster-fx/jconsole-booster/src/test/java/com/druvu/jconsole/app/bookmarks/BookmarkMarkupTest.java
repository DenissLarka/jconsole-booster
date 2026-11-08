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
    public void aLabelSplitsIntoStretchesOfOneLook() {
        assertThat(BookmarkMarkup.runs("[Orange staging 1] and *b*"))
                .containsExactly(
                        new BookmarkMarkup.Run("staging 1", false, "#d28232"),
                        new BookmarkMarkup.Run(" and ", false, null),
                        new BookmarkMarkup.Run("b", true, null));
    }

    @Test
    public void boldAndColourNestEitherWay() {
        BookmarkMarkup.Run boldRed = new BookmarkMarkup.Run("prod", true, "#c83232");

        assertThat(BookmarkMarkup.runs("[red *prod*]")).containsExactly(boldRed);
        assertThat(BookmarkMarkup.runs("*[red prod]*")).containsExactly(boldRed);
    }

    @Test
    public void blackIsTheThemesTextColour() {
        assertThat(BookmarkMarkup.runs("[black prod]")).containsExactly(new BookmarkMarkup.Run("prod", false, null));
    }

    @Test
    public void markupAfterAnUnknownColourStillApplies() {
        assertThat(BookmarkMarkup.runs("[pink x] *y*"))
                .containsExactly(
                        new BookmarkMarkup.Run("[pink x] ", false, null), new BookmarkMarkup.Run("y", true, null));
    }

    @Test
    public void plainTextAndNothingStayAsTheyAre() {
        assertThat(BookmarkMarkup.plain("local sample target")).isEqualTo("local sample target");
        assertThat(BookmarkMarkup.plain(null)).isEmpty();
    }
}
